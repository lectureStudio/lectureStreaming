package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.dto.CourseDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.CourseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseStates courseStates;


	@RequestMapping("/")
	public String index(Principal principal, Authentication authentication, Model model) {
		return nonNull(principal) ? home(authentication, model) : "index";
	}

	@GetMapping(value = "/auth")
	public String handleSamlAuth() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (nonNull(auth)) {
			return "redirect:/";
		}

		return "/";
	}

	@RequestMapping("/home")
	public String home(Authentication authentication, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		List<CourseDto> courses = new ArrayList<>();

		final int pageSize = 15;
		final Page<Course> page = courseService.getPaginated(1, pageSize, "title", "asc");

		page.getContent().forEach(course -> {
			List<UserDto> authors = new ArrayList<>();
			CourseMessageFeature messageFeature = null;
			CourseQuizFeature quizFeature = null;
			boolean canDelete = courseService.isAuthorized(course.getId(), authentication, "COURSE_DELETE");
			boolean canEdit = courseService.isAuthorized(course.getId(), authentication, "COURSE_EDIT");

			for (var registration : course.getRegistrations()) {
				User user = registration.getUser();

				if (user.getUserId().equals(details.getUsername())) {
					// If user is the author of the registered course.
					// Extensible: add more fine grained authorization.
					canDelete = true;
					canEdit = true;
				}

				authors.add(UserDto.builder()
					.familyName(user.getFamilyName())
					.firstName(user.getFirstName())
					.build());
			}

			for (var feature : course.getFeatures()) {
				if (feature instanceof CourseMessageFeature) {
					messageFeature = new CourseMessageFeature();
				}
				else if (feature instanceof CourseQuizFeature) {
					quizFeature = new CourseQuizFeature();
				}
			}

			String uri = WebMvcLinkBuilder.linkTo(CourseController.class)
				.slash(course.getId())
				.toUriComponentsBuilder()
				.queryParamIfPresent("pass", Optional.ofNullable(courseService.getHashedPasscode(course)))
				.build().encode().toUri().toString();

			CourseState state = courseStates.getCourseState(course.getId());

			courses.add(CourseDto.builder()
				.id(course.getId())
				.createdTimestamp(nonNull(state) ? state.getCreatedTimestamp() : null)
				.title(course.getTitle())
				.description(course.getDescription())
				.authors(authors)
				.messageFeature(messageFeature)
				.quizFeature(quizFeature)
				.url(uri)
				.isConference(course.isConference())
				.isProtected(nonNull(course.getPasscode()) && !course.getPasscode().isEmpty())
				.isLive(nonNull(state))
				.isRecorded(nonNull(state) ? state.getRecordedState() : false)
				.canDelete(canDelete)
				.canEdit(canEdit)
				.build());
		});

		model.addAttribute("courses", courses);

		return "home";
	}

	@GetMapping(value = "/contact")
	public String contact() {
		return "contact";
	}

	@GetMapping(value = "/sponsors")
	public String sponsors() {
		return "sponsors";
	}

	@GetMapping(value = "/imprint")
	public String manual() {
		return "imprint";
	}

	@GetMapping(value = "/privacy")
	public String privacy() {
		return "privacy";
	}
}
