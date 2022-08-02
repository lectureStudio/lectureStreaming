package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.RandomStringUtils;

import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseCredentials;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseRegistration;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.CourseForm.CourseFormPrivilege;
import org.lecturestudio.web.portal.model.CourseForm.CourseFormRole;
import org.lecturestudio.web.portal.model.dto.CourseDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.util.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.util.Streamable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/course")
public class CourseController {

	@Autowired
	private UserService userService;

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private MessageSource messageSource;


	@RequestMapping("/{id}")
	public String showCourse(@PathVariable("id") long id, @RequestParam(required = false) String pass, Authentication authentication, Model model) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new CourseNotFoundException());

		boolean isProtected = nonNull(course.getPasscode()) && !course.getPasscode().isEmpty();

		List<UserDto> authors = new ArrayList<>();
		CourseMessageFeature messageFeature = null;
		CourseQuizFeature quizFeature = null;

		for (var registration : course.getRegistrations()) {
			User user = registration.getUser();

			authors.add(UserDto.builder()
				.familyName(user.getFamilyName())
				.firstName(user.getFirstName())
				.build());
		}

		for (var feature : course.getFeatures()) {
			if (feature instanceof CourseMessageFeature) {
				messageFeature = new CourseMessageFeature();
				messageFeature.setFeatureId(feature.getFeatureId());
			}
			else if (feature instanceof CourseQuizFeature) {
				quizFeature = new CourseQuizFeature();
				quizFeature.setFeatureId(feature.getFeatureId());
			}
		}

		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		CourseState state = courseStates.getCourseState(course.getId());

		CourseDto courseDto = CourseDto.builder()
			.id(course.getId())
			.userId(details.getUsername())
			.roomId(course.getRoomId())
			.createdTimestamp(nonNull(state) ? state.getCreatedTimestamp() : null)
			.title(course.getTitle())
			.description(course.getDescription())
			.authors(authors)
			.messageFeature(messageFeature)
			.quizFeature(quizFeature)
			.isLive(nonNull(state))
			.isRecorded(nonNull(state) ? state.getRecordedState() : false)
			.isProtected(isProtected)
			.build();

		Map<String, String> mediaProfiles = new HashMap<>();
		mediaProfiles.put("home", messageSource.getMessage("settings.media.profile.home", null, LocaleContextHolder.getLocale()));
		mediaProfiles.put("classroom", messageSource.getMessage("settings.media.profile.classroom", null, LocaleContextHolder.getLocale()));

		model.addAttribute("course", courseDto);
		model.addAttribute("profiles", mediaProfiles);

		// Check course credentials.
		Object credentials = model.getAttribute("credentials");
		boolean needsInput = isProtected;

		if (isProtected && nonNull(pass) && !pass.isEmpty()) {
			needsInput = !courseService.hasSameHashedPasscode(course, pass);

			if (needsInput) {
				// Entered wrong passcode.
				model.addAttribute("passcodeError", Boolean.TRUE);
			}
		}
		if (nonNull(credentials) && credentials instanceof CourseCredentials) {
			CourseCredentials cred = (CourseCredentials) credentials;
			needsInput = !course.getPasscode().equals(cred.getPasscode());

			if (needsInput) {
				// Entered wrong passcode.
				model.addAttribute("passcodeError", Boolean.TRUE);
			}
		}

		if (needsInput) {
			model.addAttribute("credentials", new CourseCredentials());

			return "course-passcode";
		}

		return "course";
	}

	@PostMapping("/passcode/{id}")
	public String getCoursePasscode(@PathVariable("id") long id, @Valid CourseCredentials credentials, BindingResult result, RedirectAttributes redirectAttrs) {
		if (result.hasErrors()) {
			return "course-passcode";
		}

		redirectAttrs.addAttribute("id", id);
		redirectAttrs.addFlashAttribute("credentials", credentials);

		return "redirect:/course/{id}";
	}

	@RequestMapping("/new")
	public String addCourse(Authentication authentication, Model model) {
		CourseForm courseForm = courseService.createCourseForm();

		List<String> users = Streamable.of(userService.getAllUsers())
			.filter((user) -> {
				return !user.getUserId().equals(authentication.getName());
			})
			.map((user) -> {
				return user.getUserId();
			})
			.toList();

		model.addAttribute("courseForm", courseForm);
		model.addAttribute("edit", false);
		model.addAttribute("users", users);
		model.addAttribute("canAlterPrivileges", true);

		return "course-form";
	}

	@PostMapping("/new")
	public String newCourse(Authentication authentication, HttpServletRequest request, @Valid CourseForm courseForm, BindingResult result, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		if (result.hasErrors()) {
			List<String> users = Streamable.of(userService.getAllUsers())
				.filter((user) -> {
					return !user.getUserId().equals(authentication.getName());
				})
				.map((user) -> {
					return user.getUserId();
				})
				.toList();

			model.addAttribute("edit", false);
			model.addAttribute("users", users);
			model.addAttribute("edit", false);
			model.addAttribute("canAlterPrivileges", true);

			return "course-form";
		}

		String baseUri = request.getScheme() + "://" + request.getServerName();

		Course course = new Course();
		course.setRoomId(RandomStringUtils.randomAlphanumeric(17));
		course.setDescription(StringUtils.cleanHtml(courseForm.getDescription(), baseUri));
		course.setTitle(courseForm.getTitle());
		course.setPasscode(courseForm.getPasscode());

		Set<CourseRole> courseRoles = new HashSet<>();

		for (CourseFormRole formRole : courseForm.getRoles()) {
			Set<CoursePrivilege> coursePrivileges = new HashSet<>();

			for (CourseFormPrivilege formPrivilege : formRole.getPrivileges()) {
				if (formPrivilege.isSelected()) {
					coursePrivileges.add(CoursePrivilege.builder()
							.privilege(formPrivilege.getPrivilege())
							.build());
				}
			}

			courseRoles.add(CourseRole.builder()
					.course(course)
					.role(formRole.getRole())
					.privileges(coursePrivileges)
					.build());
		}

		course.setRoles(courseRoles);

		User user = userService.findById(details.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		CourseRegistration registration = CourseRegistration.builder()
				.user(user)
				.course(course)
				.build();

		course.setRegistrations(Set.of(registration));
		user.getRegistrations().add(registration);

		courseService.saveCourse(course);
		userService.saveUser(user);

		return "redirect:/";
	}

	@RequestMapping("/edit/{id}")
	public String editCourse(@PathVariable("id") long id, Authentication authentication, Model model) {
		courseService.isAuthorized(id, authentication, "COURSE_EDIT");

		Course course = courseService.findById(id)
				.orElseThrow(() -> new CourseNotFoundException());

		boolean canAlterPrivileges = courseService.isAuthorized(id, authentication, "COURSE_ALTER_PRIVILEGES");

		CourseForm courseForm = courseService.createCourseForm(course);

		List<String> users = Streamable.of(userService.getAllUsers())
			.filter((user) -> {
				return !user.getUserId().equals(authentication.getName());
			})
			.map((user) -> {
				return user.getUserId();
			})
			.toList();

		model.addAttribute("courseForm", courseForm);
		model.addAttribute("edit", true);
		model.addAttribute("users", users);
		model.addAttribute("canAlterPrivileges", canAlterPrivileges);

		return "course-form";
	}

	@PostMapping("/edit/{id}")
	public String updateCourse(Authentication authentication, HttpServletRequest request, @PathVariable("id") long id, @Valid CourseForm courseForm,
			BindingResult result, Model model) {
		courseService.isAuthorized(id, authentication, "COURSE_EDIT");

		Course dbCourse = courseService.findById(id)
				.orElseThrow(() -> new CourseNotFoundException());

		boolean canAlterPrivileges = courseService.isAuthorized(id, authentication, "COURSE_ALTER_PRIVILEGES");

		if (result.hasErrors()) {
			List<String> users = Streamable.of(userService.getAllUsers())
					.filter((user) -> {
						return !user.getUserId().equals(authentication.getName());
					})
					.map((user) -> {
						return user.getUserId();
					})
					.toList();

			model.addAttribute("edit", true);
			model.addAttribute("users", users);
			model.addAttribute("canAlterPrivileges", canAlterPrivileges);

			return "course-form";
		}

		String baseUri = request.getScheme() + "://" + request.getServerName();

		dbCourse.setTitle(courseForm.getTitle());
		dbCourse.setDescription(StringUtils.cleanHtml(courseForm.getDescription(), baseUri));
		dbCourse.setPasscode(courseForm.getPasscode());

		if (canAlterPrivileges) {
			Set<CourseRole> courseRoles = new HashSet<>();

			for (CourseFormRole formRole : courseForm.getRoles()) {
				Set<CoursePrivilege> coursePrivileges = new HashSet<>();

				for (CourseFormPrivilege formPrivilege : formRole.getPrivileges()) {
					if (formPrivilege.isSelected()) {
						coursePrivileges.add(CoursePrivilege.builder()
								.privilege(formPrivilege.getPrivilege())
								.build());
					}
				}

				courseRoles.add(CourseRole.builder()
						.course(dbCourse)
						.role(formRole.getRole())
						.privileges(coursePrivileges)
						.build());
			}

			dbCourse.setRoles(courseRoles);
		}

		courseService.saveCourse(dbCourse);

		return "redirect:/";
	}

	@PostMapping("/delete/{id}")
	public String deleteCourse(Authentication authentication, @PathVariable("id") long id, Model model) {
		courseService.isAuthorized(id, authentication, "COURSE_DELETE");
		courseService.deleteById(id);

		return "redirect:/";
	}

	@PostMapping("/new/addUser")
	public String newCourseAddUser(Authentication authentication, @Valid CourseForm courseForm, BindingResult result, Model model) {
		// addUserToPersonalPrivilegeSelection(authentication, courseForm, result, model, false);

		return "course-form";
	}

	// @PostMapping("/edit/{id}/addUser")
	// public String updateCourseAddUser(Authentication authentication, @PathVariable("id") long id, @Valid CourseForm courseForm,
	// 		BindingResult result, Model model) {
	// 	Course course = courseService.findById(id)
	// 			.orElseThrow(() -> new CourseNotFoundException());

	// 	LectUserDetails details = (LectUserDetails) authentication.getDetails();

	// 	courseForm.setId(course.getId());

	// 	Optional<CoursePrivilege> requiredPrivilege = roleService.findByPrivilegeName("ALTER_PRIVILEGES_PRIVILEGE");
	// 	if (requiredPrivilege.isPresent()) {
	// 		roleService.checkAuthorization(course, details, requiredPrivilege.get());
	// 	}

	// 	addUserToPersonalPrivilegeSelection(authentication, courseForm, result, model, true);

	// 	return "course-form";
	// }

	// @PostMapping("/new/removeUser/{userId}")
	// public String newCourseRemoveUser(Authentication authentication, @PathVariable("userId")  String userId, @Valid CourseForm courseForm, BindingResult result, Model model) {
	// 	removeUserFromPersonalPrivilegeSelection(authentication, null, userId, courseForm, result, model, false);

	// 	return "course-form";
	// }

	// @PostMapping("/edit/{id}/removeUser/{userId}")
	// public String updateCourseRemoveUser(Authentication authentication, @PathVariable("id") long id, @PathVariable("userId") String userId, @Valid CourseForm courseForm,
	// 		BindingResult result, Model model) {
	// 	Course course = courseService.findById(id)
	// 		.orElseThrow(() -> new CourseNotFoundException());

	// 	LectUserDetails details = (LectUserDetails) authentication.getDetails();

	// 	Optional<CoursePrivilege> requiredPrivilege = roleService.findByPrivilegeName("ALTER_PRIVILEGES_PRIVILEGE");
	// 	if (requiredPrivilege.isPresent()) {
	// 		roleService.checkAuthorization(course, details, requiredPrivilege.get());
	// 	}
 
	// 	removeUserFromPersonalPrivilegeSelection(authentication, id, userId, courseForm, result, model, true);

	// 	return "course-form";
	// }
}
