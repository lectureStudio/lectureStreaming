package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.apache.commons.lang3.RandomStringUtils;
import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseCredentials;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseRegistration;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.CourseRolesFormDataSink;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.PrivilegeFormDataSink;
import org.lecturestudio.web.portal.model.CourseStates;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/course")
public class CourseController {

	@Autowired
	private UserService userService;

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseRegistrationService registrationService;

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private LocaleResolver localeResolver;

	@Autowired
	private RoleService roleService;

	private final Map<String, String> dict = new HashMap<>();

	private final List<String> DICT_KEYS = List.of(
		"course.feature.message.sent",
		"course.feature.message.send.error",
		"course.feature.quiz.sent",
		"course.feature.quiz.send.error",
		"course.feature.quiz.count.error",
		"course.feature.quiz.input.invalid",
		"course.speech.request.speak",
		"course.speech.request.ended",
		"course.speech.request.rejected",
		"course.feature.message.reply.tooltip",
		"course.feature.message.form.placeholder",
		"course.feature.message.public",
		"course.feature.message.private",
		"course.feature.message.destination.all",
		"course.feature.message.destination.lecturer"
	);


	@RequestMapping("/{id}")
	public String showCourse(@PathVariable("id") long id, @RequestParam(required = false) String pass, Authentication authentication, Model model) {
		for (String key : DICT_KEYS) {
			dict.put(key, messageSource.getMessage(key, null, LocaleContextHolder.getLocale()));
		}

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
			.isProtected(isProtected)
			.build();

		model.addAttribute("course", courseDto);
		model.addAttribute("dict", dict);

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

		model.addAttribute("courseForm", courseForm);
		model.addAttribute("edit", false);

		return "course-form";
	}

	@PostMapping("/new")
	public String newCourse(Authentication authentication, @Valid CourseForm courseForm, BindingResult result, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();
 
		if (result.hasErrors()) {
			model.addAttribute("edit", false);

			return "course-form";
		}

		Course course = new Course();

		course.setRoomId(RandomStringUtils.randomAlphanumeric(17));
		course.setDescription(StringUtils.cleanHtml(courseForm.getDescription()));
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

		courseService.saveCourse(course);
		userService.saveUser(user);
		roleService.flushCourseFormRoles(course, courseForm);

		return "redirect:/";
	}

	@RequestMapping("/edit/{id}")
	public String editCourse(@PathVariable("id") long id, Authentication authentication, Model model) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		checkAuthorization(course.getId(), details);

		CourseForm courseForm = courseService.getCourseForm(course);

		model.addAttribute("courseForm", courseForm);
		model.addAttribute("edit", true);

		return "course-form";
	}

	@PostMapping("/edit/{id}")
	public String updateCourse(Authentication authentication, @PathVariable("id") long id, @Valid CourseForm courseForm,
			BindingResult result, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		checkAuthorization(id, details);

		if (result.hasErrors()) {
			model.addAttribute("edit", true);

			return "course-form";
		}

		Course dbCourse = courseService.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));
		dbCourse.setTitle(courseForm.getTitle());
		dbCourse.setDescription(StringUtils.cleanHtml(courseForm.getDescription()));
		dbCourse.setPasscode(courseForm.getPasscode());

		courseService.saveCourse(dbCourse);
		roleService.flushCourseFormRoles(dbCourse, courseForm);

		return "redirect:/";
	}

	@PostMapping("/delete/{id}")
	public String deleteCourse(Authentication authentication, @PathVariable("id") long id, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		checkAuthorization(id, details);

		courseService.deleteById(id);

		return "redirect:/";
	}

	@RequestMapping("/messenger/{id}")
	public String getMessenger(@PathVariable("id") long id, Model model) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));

		CourseMessageFeature messageFeature = null;

		for (var feature : course.getFeatures()) {
			if (feature instanceof CourseMessageFeature) {
				messageFeature = new CourseMessageFeature();
				messageFeature.setFeatureId(feature.getFeatureId());
			}
		}

		CourseDto courseDto = CourseDto.builder()
			.id(course.getId())
			.messageFeature(messageFeature)
			.build();

		model.addAttribute("course", courseDto);

		return "fragments/messenger :: messenger";
	}

	@RequestMapping("/messenger/messageReceived")
	public String getMessageReceived(@RequestParam("timestamp") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date, @RequestParam("content") String content,
		@RequestParam("from") String from, @RequestParam("id") String id, @RequestParam("messageType") String messageType, @RequestParam("to") String to, Authentication authentication) {

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		String time = String.format("%02d:%02d", date.getHour(), date.getMinute());
		StringBuilder destinationStringBuilder = new StringBuilder();
		if (!to.isEmpty()) {
			Optional<User> destinationUser = userService.findById(to);
			if (destinationUser.isPresent()) {
				destinationStringBuilder.append(destinationUser.get().getFirstName());
				destinationStringBuilder.append(" ");
				destinationStringBuilder.append(destinationUser.get().getFamilyName());
			}
		}

		StringBuilder fromStringBuilder = new StringBuilder();
		if (!from.isEmpty()) {
			Optional<User> fromUser = userService.findById(from);
			if (fromUser.isPresent()) {
				fromStringBuilder.append(fromUser.get().getFirstName());
				fromStringBuilder.append(" ");
				fromStringBuilder.append(fromUser.get().getFamilyName());
			}
		}
		if (details.getUsername().equals(from)) {
			return String.format("fragments/messenger-message :: messenger-message(id='%s', timestamp='%s', content='%s', messageType='%s', to='%s')", id, time, content, messageType, destinationStringBuilder.toString());
		}
		else {
			return String.format("fragments/messenger-other-message :: messenger-other-message(id='%s', timestamp='%s', content='%s', from='%s', messageType='%s', to='%s')", id, time, content, fromStringBuilder.toString(), messageType, destinationStringBuilder.toString());
		}
	}

	@RequestMapping("/quiz/{id}")
	public String getQuiz(@PathVariable("id") long id, Model model) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + id));

		CourseQuizFeature quizFeature = null;

		for (var feature : course.getFeatures()) {
			if (feature instanceof CourseQuizFeature) {
				CourseQuizFeature qf = (CourseQuizFeature) feature;

				quizFeature = new CourseQuizFeature();
				quizFeature.setFeatureId(qf.getFeatureId());
				quizFeature.setQuestion(qf.getQuestion());
				quizFeature.setType(qf.getType());
				quizFeature.setOptions(qf.getOptions());
			}
		}

		CourseDto courseDto = CourseDto.builder()
			.id(course.getId())
			.quizFeature(quizFeature)
			.build();

		model.addAttribute("course", courseDto);

		return "fragments/quiz :: quiz";
	}

	private void checkAuthorization(Long courseId, LectUserDetails details) {
		registrationService.findByCourseAndUserId(courseId, details.getUsername())
				.orElseThrow(() -> new UnauthorizedException());
	}
}
