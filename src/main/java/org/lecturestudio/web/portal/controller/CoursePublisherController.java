package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.lecturestudio.web.api.model.quiz.Quiz;
import org.lecturestudio.web.api.stream.model.CoursePresenceType;
import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.exception.FeatureNotFoundException;
import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseEvent;
import org.lecturestudio.web.portal.model.CourseFeature;
import org.lecturestudio.web.portal.model.CourseFeatureEvent;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.ChatHistoryService;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseQuizResource;
import org.lecturestudio.web.portal.model.CourseSpeechEvent;
import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.dto.CourseDto;
import org.lecturestudio.web.portal.model.dto.CourseFeatureDto;
import org.lecturestudio.web.portal.model.dto.CourseQuizFeatureDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.model.dto.UserPrivilegesDto;
import org.lecturestudio.web.portal.property.SimpProperties;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.util.SimpEmitter;
import org.lecturestudio.web.portal.util.StringUtils;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.service.CourseParticipantService;
import org.lecturestudio.web.portal.service.CoursePresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/publisher")
public class CoursePublisherController {

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseFeatureService courseFeatureService;

	@Autowired
	private CourseSpeechRequestService speechRequestService;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private CourseParticipantService participantService;

	@Autowired
	private CoursePresenceService presenceService;

	@Autowired
	private UserService userService;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private SimpProperties simpProperties;

	@Autowired
	private SimpEmitter simpEmitter;


	@GetMapping("/user")
	public UserDto getUser(Authentication authentication) {
		User user = userService.findById(authentication.getName())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		UserDto userDto = UserDto.builder()
				.userId(authentication.getName())
				.familyName(user.getFamilyName())
				.firstName(user.getFirstName())
				.build();

		return userDto;
	}

	@GetMapping("/user/privileges/{courseId}")
	public UserPrivilegesDto getUserPrivileges(@PathVariable("courseId") long courseId, Authentication authentication) {
		Set<Privilege> privileges = courseService.getUserPrivileges(courseId, authentication.getName());

		UserPrivilegesDto privilegesDto = UserPrivilegesDto.builder()
				.privileges(privileges)
				.build();

		return privilegesDto;
	}

	@GetMapping("/participants/{courseId}")
	public List<UserDto> getParticipants(@PathVariable("courseId") Long courseId, Authentication authentication) {
		List<UserDto> participants = new ArrayList<>();

		if (!courseService.isAuthorized(courseId, authentication, "PARTICIPANTS_VIEW")) {
			return participants;
		}

		participantService.findAllUsersByCourseId(courseId).forEach(user -> {
			UserDto participant = UserDto.builder()
					.userId(user.getUserId())
					.familyName(user.getFamilyName())
					.firstName(user.getFirstName())
					.participantType(presenceService.getCourseParticipantType(user, courseId))
					.build();

			CoursePresenceType pType = presenceService.getPresenceType(user, courseId);

			if (nonNull(pType)) {
				participant.setPresenceType(pType);
			}

			if (!participants.contains(participant)) {
				participants.add(participant);
			}
		});

		return participants;
	}

	@GetMapping("/courses")
	public List<CourseDto> getCourses(Authentication authentication) {
		User user = userService.findById(authentication.getName())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		List<Course> courses = new ArrayList<>();
		List<CourseDto> coursesDto = new ArrayList<>();

		courseService.findAllByUserId(user.getUserId()).forEach(courses::add);
		courses.addAll(courseService.findAllCourses(user.getUserId()));

		courses.forEach(course -> {
			if (courseService.isAuthorized(user, course, "COURSE_STREAM")) {
				coursesDto.add(CourseDto.builder()
						.id(course.getId())
						.roomId(course.getRoomId())
						.title(course.getTitle())
						.description(course.getDescription())
						.isProtected(nonNull(course.getPasscode()) && !course.getPasscode().isEmpty())
						.build());
			}
		});

		return coursesDto;
	}

	@PostMapping("/file/upload")
	public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file, Authentication authentication) {
		String fileName = fileStorageService.save(file);

		String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/course/file/")
			.path(fileName)
			.toUriString();

		return ResponseEntity.status(HttpStatus.OK).body(fileDownloadUri);
	}

	@PostMapping("/speech/accept/{requestId}")
	public ResponseEntity<Void> acceptSpeech(@PathVariable("requestId") long requestId, Authentication authentication) {
		Optional<CourseSpeechRequest> speechRequestOpt = speechRequestService.findByRequestId(requestId);

		if (speechRequestOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).build();
		}

		CourseSpeechRequest speechRequest = speechRequestOpt.get();
		long courseId = speechRequest.getCourseId();

		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		CourseSpeechEvent courseEvent = CourseSpeechEvent.builder()
			.courseId(courseId)
			.requestId(BigInteger.valueOf(speechRequest.getRequestId()))
			.accepted(true)
			.build();

		simpEmitter.emmitEventToUser(courseId, simpProperties.getEvents().getSpeech(), courseEvent, speechRequest.getUserId());

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/speech/reject/{requestId}")
	public ResponseEntity<Void> rejectSpeech(@PathVariable("requestId") long requestId, Authentication authentication) {
		Optional<CourseSpeechRequest> speechRequestOpt = speechRequestService.findByRequestId(requestId);

		if (speechRequestOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).build();
		}

		CourseSpeechRequest speechRequest = speechRequestOpt.get();
		long courseId = speechRequest.getCourseId();

		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		speechRequestService.deleteById(speechRequest.getId());

		CourseSpeechEvent courseEvent = CourseSpeechEvent.builder()
			.courseId(courseId)
			.requestId(BigInteger.valueOf(speechRequest.getRequestId()))
			.accepted(false)
			.build();

		simpEmitter.emmitEventToUser(courseId, simpProperties.getEvents().getSpeech(), courseEvent, speechRequest.getUserId());

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/course/recorded/{courseId}/{recorded}")
	public ResponseEntity<Void> setCourseRecordingState(@PathVariable("courseId") long courseId,
			@PathVariable("recorded") boolean isRecorded, Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		CourseState courseState = courseStates.getCourseState(courseId);
		courseState.setRecordedState(isRecorded);

		CourseEvent courseEvent = CourseEvent.builder()
				.courseId(courseId)
				.createdTimestamp(System.currentTimeMillis())
				.started(isRecorded)
				.build();

		simpEmitter.emmitEventAndAll(courseId, simpProperties.getEvents().getRecording(), courseEvent);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/messenger/start/{courseId}")
	public ResponseEntity<String> startMessenger(@PathVariable("courseId") long courseId, Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		return startFeature(courseId, new CourseMessageFeature(), new CourseFeatureDto(), authentication);
	}

	@PostMapping("/messenger/stop/{courseId}")
	public ResponseEntity<String> stopMessenger(@PathVariable("courseId") long courseId, Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		return stopFeature(courseId, CourseMessageFeature.class);
	}

	@PostMapping("/quiz/start/{courseId}")
	public ResponseEntity<String> startQuiz(@PathVariable("courseId") long courseId, @RequestBody Quiz quiz, Authentication authentication, HttpServletRequest request) {
		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		String baseUri = request.getScheme() + "://" + request.getServerName();

		CourseQuizFeature feature = new CourseQuizFeature();
		feature.setQuestion(StringUtils.cleanHtml(quiz.getQuestion(), baseUri));
		feature.setType(quiz.getType());
		feature.setOptions(quiz.getOptions());

		CourseFeatureDto dto = CourseQuizFeatureDto.builder()
				.type(feature.getType())
				.question(feature.getQuestion().replace("&#xa0;"," "))
				.options(feature.getOptions())
				.build();

		return startFeature(courseId, feature, dto, authentication);
	}

	@PostMapping(
		value = "/v2/quiz/start/{courseId}",
		consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }
	)
	public ResponseEntity<String> startQuiz(HttpServletRequest request, @PathVariable("courseId") long courseId,
			@RequestPart("quiz") Quiz quiz, @RequestPart("files") Optional<MultipartFile[]> files,
			Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		String baseUri = request.getScheme() + "://" + request.getServerName();
		CourseQuizFeature feature = new CourseQuizFeature();
		List<CourseQuizResource> resources = new ArrayList<>();

		if (files.isPresent()) {
			try {
				for (MultipartFile file : files.get()) {
					CourseQuizResource resource = new CourseQuizResource();
					resource.setName(file.getOriginalFilename());
					resource.setType(file.getContentType());
					resource.setContent(file.getBytes());
					resource.setCourseId(courseId);
					resource.setFeature(feature);

					resources.add(resource);
				}
			}
			catch (Throwable e) {
				return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Failed to upload files");
			}
		}

		feature.setQuestion(StringUtils.cleanHtml(quiz.getQuestion(), baseUri));
		feature.setType(quiz.getType());
		feature.setOptions(quiz.getOptions());
		feature.setResources(resources);

		CourseFeatureDto dto = CourseQuizFeatureDto.builder()
				.type(feature.getType())
				.question(feature.getQuestion().replace("&#xa0;"," "))
				.options(feature.getOptions())
				.build();

		return startFeature(courseId, feature, dto, authentication);
	}

	@PostMapping("/quiz/stop/{courseId}")
	public ResponseEntity<String> stopQuiz(@PathVariable("courseId") long courseId, Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "COURSE_STREAM")) {
			throw new UnauthorizedException();
		}

		return stopFeature(courseId, CourseQuizFeature.class);
	}

	ResponseEntity<String> startFeature(long courseId, CourseFeature feature, CourseFeatureDto dto, Authentication authentication) {
		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		CourseFeature courseFeature = course.getFeatures()
				.stream()
				.filter(s -> s.getClass().equals(feature.getClass()))
				.findFirst().orElse(null);

		if (nonNull(courseFeature)) {
			// Delete and override feature.
			course.getFeatures().remove(courseFeature);

			courseFeatureService.deleteById(courseFeature.getId());
		}

		User initiator = userService.findById(authentication.getName())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		feature.setInitiator(initiator);
		feature.setCourse(course);
		feature.setFeatureId(Long.toString(new SecureRandom().nextLong()));

		dto.setFeatureId(feature.getFeatureId());

		course.getFeatures().add(feature);

		courseService.saveCourse(course);

		if (feature instanceof CourseMessageFeature) {
			chatHistoryService.createCourseHistory(courseId);
		}

		// Send feature state event.
		sendFeatureState(course.getId(), dto, feature.getName(), true);

		return ResponseEntity.status(HttpStatus.OK).body(feature.getFeatureId());
	}

	ResponseEntity<String> stopFeature(long courseId, Class<? extends CourseFeature> featureClass) {
		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		CourseFeature courseFeature = course.getFeatures()
				.stream()
				.filter(s -> s.getClass().equals(featureClass))
				.findFirst().orElse(null);

		if (isNull(courseFeature)) {
			throw new FeatureNotFoundException();
		}

		course.getFeatures().remove(courseFeature);
		courseService.saveCourse(course);

		courseFeatureService.deleteById(courseFeature.getId());

		if (courseFeature instanceof CourseMessageFeature) {
			chatHistoryService.removeCourseHistory(courseId);
		}

		// Send feature state event.
		CourseFeatureDto dto = new CourseFeatureDto();
		dto.setFeatureId(courseFeature.getFeatureId());

		sendFeatureState(course.getId(), dto, courseFeature.getName(), false);

		return ResponseEntity.status(HttpStatus.OK).body(courseFeature.getFeatureId());
	}

	void sendFeatureState(long courseId, CourseFeatureDto feature, String name, boolean started) {
		CourseFeatureEvent courseEvent = CourseFeatureEvent.builder()
			.courseId(courseId)
			.started(started)
			.feature(feature)
			.build();

		simpEmitter.emmitEventAndAll(courseId, name, courseEvent);
	}
}
