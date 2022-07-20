package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.RandomStringUtils;
import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseCredentials;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseRegistration;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.CourseUserId;
import org.lecturestudio.web.portal.model.PrivilegeFormDataSink;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.dto.CourseDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.CourseRegistrationService;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.RoleService;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.util.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.util.Streamable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
	private RoleService roleService;

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseRegistrationService registrationService;

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private MessageSource messageSource;


	@PostConstruct
	public void postConstruct() {
		courseStates.addCourseStateListener(roleService);
	}

	@RequestMapping("/{id}")
	public String showCourse(@PathVariable("id") long id, @RequestParam(required = false) String pass, Authentication authentication, Model model) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));

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
		CourseForm courseForm = courseService.getEmptyCourseForm();

		List<String> users = Streamable.of(userService.getAllUsers())
			.filter((user) -> {
				return !user.getUserId().equals(authentication.getName());
			})
			.map((user) -> {
				return user.getUserId();
			})
			.toList();

		courseForm.getCourseRoles().forEach(System.out::println);

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

		User user = userService.findById(details.getUsername())
			.orElseThrow(() -> new IllegalArgumentException("User is not present"));

		CourseRegistration registration = CourseRegistration.builder()
			.user(user)
			.course(course)
			.build();

		course.getRegistrations().add(registration);
		user.getRegistrations().add(registration);

		courseForm.getPersonallyPrivilegedUsers().add(user);

		List<PrivilegeFormDataSink> ownerDataSinks = Streamable.of(roleService.getAllPrivileges())
			.toList()
			.stream()
			.map((p) -> {
				return new PrivilegeFormDataSink(p, true);
			})
			.toList();

		courseForm.getPrivilegeSinks().addAll(ownerDataSinks);

		courseService.saveCourse(course);
		userService.saveUser(user);

		flushPrivilegeFormDataSinks(courseForm, course);

		return "redirect:/";
	}

	@RequestMapping("/edit/{id}")
	public String editCourse(@PathVariable("id") long id, Authentication authentication, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		Course course = courseService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));

		Optional<CoursePrivilege> requiredToEditPrivilege = roleService.findByPrivilegeName("EDIT_COURSE_PRIVILEGE");
		if (requiredToEditPrivilege.isPresent()) {
			roleService.checkAuthorization(course, details, requiredToEditPrivilege.get());
		}

		boolean canAlterPrivileges = true;

		Optional<CoursePrivilege> requiredToAlterCourseRolesPrivilege = roleService.findByPrivilegeName("ALTER_PRIVILEGES_PRIVILEGE");
		if (requiredToAlterCourseRolesPrivilege.isPresent()) {
			try {
				roleService.checkAuthorization(course, details, requiredToAlterCourseRolesPrivilege.get());
			}
			catch (UnauthorizedException exc) {
				canAlterPrivileges = false;
			}
		}

		CourseForm courseForm = courseService.getCourseForm(course);

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
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		Course dbCourse = courseService.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));
		boolean canAlterPrivileges = true;

		Optional<CoursePrivilege> requiredToAlterPrivileges = roleService.findByPrivilegeName("ALTER_COURSE_ROLES_PRIVILEGE");
		if (requiredToAlterPrivileges.isPresent()) {
			canAlterPrivileges = roleService.isAuthorized(dbCourse, details, requiredToAlterPrivileges.get());
		}

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

		Optional<CoursePrivilege> requiredToEditPrivilege = roleService.findByPrivilegeName("EDIT_COURSE_PRIVILEGE");
		if (requiredToEditPrivilege.isPresent()) {
			roleService.checkAuthorization(dbCourse, details, requiredToEditPrivilege.get());
		}

		dbCourse.setTitle(courseForm.getTitle());
		dbCourse.setDescription(StringUtils.cleanHtml(courseForm.getDescription(), baseUri));
		dbCourse.setPasscode(courseForm.getPasscode());

		courseService.saveCourse(dbCourse);

		if (canAlterPrivileges) {
			flushPrivilegeFormDataSinks(courseForm, dbCourse);
		}

		return "redirect:/";
	}

	@PostMapping("/delete/{id}")
	public String deleteCourse(Authentication authentication, @PathVariable("id") long id, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		Course course = courseService.findById(id)
			.orElseThrow(() -> new CourseNotFoundException());

		Optional<CoursePrivilege> requiredPrivilege = roleService.findByPrivilegeName("DELETE_COURSE_PRIVILEGE");
		if (requiredPrivilege.isPresent()) {
			roleService.checkAuthorization(course, details, requiredPrivilege.get());
		}

		courseService.deleteById(id);

		return "redirect:/";
	}

	@PostMapping("/new/addUser")
	public String newCourseAddUser(Authentication authentication, @Valid CourseForm courseForm, BindingResult result, Model model) {
		addUserToPersonalPrivilegeSelection(authentication, courseForm, result, model, false);
		
		return "course-form";
	}

	@PostMapping("/edit/{id}/addUser")
	public String updateCourseAddUser(Authentication authentication, @PathVariable("id") long id, @Valid CourseForm courseForm,
			BindingResult result, Model model) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new UnauthorizedException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		courseForm.setId(course.getId());

		Optional<CoursePrivilege> requiredPrivilege = roleService.findByPrivilegeName("ALTER_PRIVILEGES_PRIVILEGE");
		if (requiredPrivilege.isPresent()) {
			roleService.checkAuthorization(course, details, requiredPrivilege.get());
		}

		addUserToPersonalPrivilegeSelection(authentication, courseForm, result, model, true);

		return "course-form";
	}

	@PostMapping("/new/removeUser/{userId}")
	public String newCourseRemoveUser(Authentication authentication, @PathVariable("userId")  String userId, @Valid CourseForm courseForm, BindingResult result, Model model) {
		removeUserFromPersonalPrivilegeSelection(authentication, null, userId, courseForm, result, model, false);

		return "course-form";
	}

	@PostMapping("/edit/{id}/removeUser/{userId}")
	public String updateCourseRemoveUser(Authentication authentication, @PathVariable("id") long id, @PathVariable("userId") String userId, @Valid CourseForm courseForm,
			BindingResult result, Model model) {
		Course course = courseService.findById(id)
			.orElseThrow(() -> new UnauthorizedException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		Optional<CoursePrivilege> requiredPrivilege = roleService.findByPrivilegeName("ALTER_PRIVILEGES_PRIVILEGE");
		if (requiredPrivilege.isPresent()) {
			roleService.checkAuthorization(course, details, requiredPrivilege.get());
		}
 
		removeUserFromPersonalPrivilegeSelection(authentication, id, userId, courseForm, result, model, true);

		return "course-form";
	}

	private void checkAuthorization(Long courseId, LectUserDetails details) {
		registrationService.findByCourseAndUserId(courseId, details.getUsername())
				.orElseThrow(() -> new UnauthorizedException());
	}

	@Transactional
	private void flushPrivilegeFormDataSinks(CourseForm courseForm, Course course) {
		if (Objects.isNull(courseForm.getPrivilegeSinks())) {
			return;
		}

		int numOfPrivileges = courseForm.getNumOfPrivileges();
		Iterator<PrivilegeFormDataSink> iter = courseForm.getPrivilegeSinks().iterator();

		BiConsumer<Iterator<PrivilegeFormDataSink>, Set<CoursePrivilege>> consumePrivilegeDataSink = new BiConsumer<Iterator<PrivilegeFormDataSink>, Set<CoursePrivilege>>() {

			@Override
			public void accept(Iterator<PrivilegeFormDataSink> iter, Set<CoursePrivilege> coursePrivileges) {
				for (int i = 0; i < numOfPrivileges; ++i) {
					if (iter.hasNext()) {
						PrivilegeFormDataSink current = iter.next();
						
						if (current.isExpressed()) {
							coursePrivileges.add(current.getPrivilege());
						}
					}
					else {
						throw new IndexOutOfBoundsException(
								"There is not enough privilege forms for given roles and number of privileges");
					}
				}
			}
		};

		for (Role role : courseForm.getCourseRoles()) {
			Set<CoursePrivilege> coursePrivileges = new HashSet<>();
			consumePrivilegeDataSink.accept(iter, coursePrivileges);
			roleService.saveCourseRole(course, role, coursePrivileges);
		}

		for (User user : courseForm.getPersonallyPrivilegedUsers()) {
			Set<CoursePrivilege> userPrivileges = new HashSet<>();
			consumePrivilegeDataSink.accept(iter, userPrivileges);
			roleService.saveCourseUser(course, user, userPrivileges);
		}
	}

	private void addUserToPersonalPrivilegeSelection(Authentication authentication, CourseForm courseForm, BindingResult result, Model model, boolean edit) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		Optional<User> optUserToAdd = userService.findById(courseForm.getUsername());

		courseForm.setUsername("");
		model.addAttribute("edit", edit);
		model.addAttribute("canAlterPrivileges", true);

		List<String> users = Streamable.of(userService.getAllUsers())
			.filter((user) -> {
				return !user.getUserId().equals(authentication.getName());
			})
			.map((user) -> {
				return user.getUserId();
			})
			.toList();

		model.addAttribute("users", users);

		String errorMessageKey = "course.form.user.error.notFound";

		if (optUserToAdd.isPresent()) {
			errorMessageKey = "course.form.user.error.owner";
			User userToAdd = optUserToAdd.get();
			boolean userAllowed = true;

			if (edit) {
				userAllowed = !registrationService.findByCourseAndUserId(courseForm.getId(), userToAdd.getUserId()).isPresent();
			}
			else {
				userAllowed = !details.getUsername().equals(userToAdd.getUserId());
			}

			if (userAllowed) {
				errorMessageKey = "course.form.user.error.already";
				List<User> personallyPrivilegeSelectedUsers = courseForm.getPersonallyPrivilegedUsers();
				if (! personallyPrivilegeSelectedUsers.contains(userToAdd)) {
					courseForm.getPersonallyPrivilegedUsers().add(userToAdd);
					List<PrivilegeFormDataSink> newUserSinks = Streamable.of(roleService.getAllPrivilegesOrderByIdAsc())
						.map(privilege -> {
							return PrivilegeFormDataSink.builder()
								.privilege(privilege)
								.expressed(false)
								.build();
						}).toList();
			
					courseForm.getPrivilegeSinks().addAll(newUserSinks);
					return;
				}
			}
		}

		result.rejectValue("username", errorMessageKey, messageSource.getMessage(errorMessageKey, null, LocaleContextHolder.getLocale()));
	}

	private void removeUserFromPersonalPrivilegeSelection(Authentication authentication, Long id, String userId, CourseForm courseForm, 
			BindingResult result, Model model, boolean edit) {
			
		model.addAttribute("edit", edit);
		model.addAttribute("canAlterPrivileges", true);

		Optional<User> userToRemoveOpt = userService.findById(userId);

		List<String> users = Streamable.of(userService.getAllUsers())
			.filter((user) -> {
				return !user.getUserId().equals(authentication.getName());
			})
			.map((user) -> {
				return user.getUserId();
			})
			.toList();

		model.addAttribute("users", users);

		if (userToRemoveOpt.isPresent()) {
			User userToRemove = userToRemoveOpt.get();
			int indx = courseForm.getPersonallyPrivilegedUsers().indexOf(userToRemove);
			if (indx != -1) {
				courseForm.getPersonallyPrivilegedUsers().remove(userToRemove);

				int roleCount = courseForm.getCourseRoles().size();
				int privilegeCount = courseForm.getNumOfPrivileges();

				int base = (roleCount + indx) * privilegeCount;

				ListIterator<PrivilegeFormDataSink> iter = courseForm.getPrivilegeSinks().listIterator(base);
				int i=0;
				while (iter.hasNext() && i < privilegeCount) {
					iter.next();
					iter.remove();
					i++;
				}

				if (edit) {
					CourseUserId courseUserId = new CourseUserId(id, userId);
					if (roleService.hasCourseUser(courseUserId)) {
						roleService.deleteCourseUser(courseUserId);
					}
				}
			}
		}
	}
}
