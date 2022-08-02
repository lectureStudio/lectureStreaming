package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.lecturestudio.web.api.message.MessengerDirectMessage;
import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.MessengerReplyMessage;
import org.lecturestudio.web.api.message.WebMessage;
import org.lecturestudio.web.api.model.quiz.Quiz;
import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.exception.FeatureNotFoundException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseEvent;
import org.lecturestudio.web.portal.model.CourseFeature;
import org.lecturestudio.web.portal.model.CourseFeatureEvent;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseQuizResource;
import org.lecturestudio.web.portal.model.CourseSpeechEvent;
import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.dto.CourseDto;
import org.lecturestudio.web.portal.model.dto.CourseFeatureDto;
import org.lecturestudio.web.portal.model.dto.CourseQuizFeatureDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry.MessengerFeatureUser;
import org.lecturestudio.web.portal.util.StringUtils;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.CourseFeatureService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
	private CourseMessengerFeatureSaveFeature messengerFeatureSaveFeature;

	@Autowired
	private MessengerFeatureUserRegistry messengerFeatureUserRegistry;

	@Autowired
	private UserService userService;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private ObjectMapper objectMapper;


	@GetMapping("/user")
	public UserDto getUser(Authentication authentication) {
		User user = userService.findById(authentication.getName())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		UserDto userDto = UserDto.builder()
				.userId(authentication.getName())
				.familyName(user.getFamilyName())
				.firstName(user.getFirstName()).build();

		return userDto;
	}

	@GetMapping("/courses")
	public List<CourseDto> getCourses(Authentication authentication) {
		List<CourseDto> courses = new ArrayList<>();

		courseService.getAllCourses().forEach(course -> {
			courses.add(CourseDto.builder()
					.id(course.getId())
					.roomId(course.getRoomId())
					.title(course.getTitle())
					.description(course.getDescription())
					.isProtected(nonNull(course.getPasscode()) && !course.getPasscode().isEmpty())
					.build());
		});

		return courses;
	}

	@PostMapping("/file/upload")
	public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
		String fileName = fileStorageService.save(file);

		String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/course/file/")
			.path(fileName)
			.toUriString();

		return ResponseEntity.status(HttpStatus.OK).body(fileDownloadUri);
	}

	@PostMapping("/speech/accept/{requestId}")
	public ResponseEntity<Void> acceptSpeech(@PathVariable("requestId") long requestId) {
		Optional<CourseSpeechRequest> speechRequestOpt = speechRequestService.findByRequestId(requestId);

		if (speechRequestOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).build();
		}

		CourseSpeechRequest speechRequest = speechRequestOpt.get();
		long courseId = speechRequest.getCourseId();

		CourseSpeechEvent courseEvent = CourseSpeechEvent.builder()
			.courseId(courseId)
			.requestId(BigInteger.valueOf(speechRequest.getRequestId()))
			.accepted(true)
			.build();

		simpMessagingTemplate.convertAndSend("/topic/course/event/all/speech", courseEvent);
		simpMessagingTemplate.convertAndSend("/topic/course/event/" + courseId + "/speech", courseEvent);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/speech/reject/{requestId}")
	public ResponseEntity<Void> rejectSpeech(@PathVariable("requestId") long requestId) {
		Optional<CourseSpeechRequest> speechRequestOpt = speechRequestService.findByRequestId(requestId);

		if (speechRequestOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).build();
		}

		CourseSpeechRequest speechRequest = speechRequestOpt.get();
		long courseId = speechRequest.getCourseId();

		speechRequestService.deleteById(speechRequest.getId());

		CourseSpeechEvent courseEvent = CourseSpeechEvent.builder()
			.courseId(courseId)
			.requestId(BigInteger.valueOf(speechRequest.getRequestId()))
			.accepted(false)
			.build();

		simpMessagingTemplate.convertAndSend("/topic/course/event/all/speech", courseEvent);
		simpMessagingTemplate.convertAndSend("/topic/course/event/" + courseId + "/speech", courseEvent);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/course/recorded/{courseId}/{recorded}")
	public ResponseEntity<Void> setCourseRecordingState(@PathVariable("courseId") long courseId,
			@PathVariable("recorded") boolean isRecorded) {
		courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		CourseState courseState = courseStates.getCourseState(courseId);
		courseState.setRecordedState(isRecorded);

		CourseEvent courseEvent = CourseEvent.builder()
				.courseId(courseId)
				.createdTimestamp(System.currentTimeMillis())
				.started(isRecorded)
				.build();

		simpMessagingTemplate.convertAndSend("/topic/course/event/all/recording", courseEvent);
		simpMessagingTemplate.convertAndSend("/topic/course/event/" + courseId + "/recording", courseEvent);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/messenger/start/{courseId}")
	public ResponseEntity<String> startMessenger(@PathVariable("courseId") long courseId, Authentication authentication) {
		return startFeature(courseId, new CourseMessageFeature(), new CourseFeatureDto(), authentication);
	}

	@PostMapping("/messenger/stop/{courseId}")
	public ResponseEntity<String> stopMessenger(@PathVariable("courseId") long courseId) {
		return stopFeature(courseId, CourseMessageFeature.class);
	}

	@MessageMapping("/message/publisher/{courseId}")
	@SendTo("/topic/course/{courseId}/chat")
	public void sendMessage(@Payload String messageString, @DestinationVariable Long courseId, Authentication authentication) throws Exception {
		CourseMessageFeature feature = (CourseMessageFeature) courseFeatureService.findMessageByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		User lecturer = feature.getInitiator();

		JsonNode jsonNode = this.objectMapper.readTree(messageString);
		String type = jsonNode.get("type").asText();
		WebMessage message;

		switch (type) {
			case "MessengerMessage":
				message = this.objectMapper.readValue(messageString, MessengerMessage.class);

				String userId = message.getUserId();

				if (isNull(userId)) {
					return;
				}

				User user = userService.findById(userId).orElse(null);

				if (isNull(user)) {
					return;
				}

				message.setFamilyName(user.getFamilyName());
				message.setFirstName(user.getFirstName());

				messengerFeatureSaveFeature.onFeatureMessage(courseId, message);
				
				simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/chat", message,
						Map.of("payloadType", "MessengerMessage"));
				break;
				
			case "MessengerReplyMessage":
				message = this.objectMapper.readValue(messageString, MessengerReplyMessage.class);
				messengerFeatureSaveFeature.onFeatureMessage(courseId, message);
				simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/chat", message,
						Map.of("payloadType", "MessengerReplyMessage"));
				break;

			case "MessengerDirectMessage":
				message = this.objectMapper.readValue(messageString, MessengerDirectMessage.class);
				messengerFeatureSaveFeature.onFeatureMessage(courseId, message);
				MessengerDirectMessage mdm = (MessengerDirectMessage) message;

				MessengerFeatureUser destinationUser = messengerFeatureUserRegistry.getUser(mdm.getRecipient());
				MessengerFeatureUser featureUser = messengerFeatureUserRegistry.getUser(authentication.getName());

				ArrayList<Set<String>> sets = new ArrayList<>();

				if (nonNull(destinationUser)) {
					sets.add(destinationUser.getAddressesInUse());
				}
				if (nonNull(featureUser)) {
					sets.add(featureUser.getAddressesInUse());
				}
				sets.add(Collections.singleton(lecturer.getUserId()));
				for (Set<String> set : sets) {
					for (String userDestination : set) {
						simpMessagingTemplate.convertAndSendToUser(userDestination, "/queue/course/" + courseId + "/chat", message,
								Map.of("payloadType", "MessengerDirectMessage"));
					}
				}
				break;
			default:
				return; // To-DO give useful output
		}
	}

	@PostMapping("/quiz/start/{courseId}")
	public ResponseEntity<String> startQuiz(@PathVariable("courseId") long courseId, @RequestBody Quiz quiz, Authentication authentication, HttpServletRequest request) {
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
			@RequestPart("quiz") Quiz quiz, @RequestPart("files") Optional<MultipartFile[]> files, Authentication authentication) {
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
	public ResponseEntity<String> stopQuiz(@PathVariable("courseId") long courseId, Authentication authenticatio) {
		return stopFeature(courseId, CourseQuizFeature.class);
	}

	ResponseEntity<String> startFeature(long courseId, CourseFeature feature, CourseFeatureDto dto, Authentication authentication) {
		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		CourseFeature courseFeature = course.getFeatures()
				.stream()
				.filter(s -> s.getClass().equals(feature.getClass()))
				.findFirst().orElse(null);

		if (isNull(courseFeature)) {
			User initiator = userService.findById(authentication.getName())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

			feature.setInitiator(initiator);
			feature.setCourse(course);
			feature.setFeatureId(Long.toString(new SecureRandom().nextLong()));

			dto.setFeatureId(feature.getFeatureId());

			course.getFeatures().add(feature);

			courseService.saveCourse(course);

			if (feature instanceof CourseMessageFeature) {
				messengerFeatureSaveFeature.addCourseHistory(courseId);
				messengerFeatureUserRegistry.registerCourse(courseId);
			}

			// Send feature state event.
			sendFeatureState(course.getId(), dto, feature.getName(), true);
		}

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
			messengerFeatureSaveFeature.removeCourseHistory(courseId);
			messengerFeatureUserRegistry.unregisterCourse(courseId);
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

		simpMessagingTemplate.convertAndSend("/topic/course/event/all/" + name, courseEvent);
		simpMessagingTemplate.convertAndSend("/topic/course/event/" + courseId + "/" + name, courseEvent);
	}
}
