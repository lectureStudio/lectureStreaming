package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.api.message.MessengerDirectMessage;
import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.MessengerReplyMessage;
import org.lecturestudio.web.api.message.WebMessage;
import org.lecturestudio.web.api.model.ClassroomServiceResponse;
import org.lecturestudio.web.api.model.Message;
import org.lecturestudio.web.api.model.quiz.Quiz;
import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.exception.FeatureNotFoundException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseEvent;
import org.lecturestudio.web.portal.model.CourseFeature;
import org.lecturestudio.web.portal.model.CourseFeatureEvent;
import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseSpeechEvent;
import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.model.dto.CourseDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry;
import org.lecturestudio.web.portal.service.SubscriberEmitterService;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry.MessengerFeatureUser;
import org.lecturestudio.web.portal.util.StringUtils;
import org.lecturestudio.web.portal.validator.MessageValidator;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.core.Authentication;
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
	private UserService userService;

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseFeatureState courseFeatureState;

	@Autowired
	private CourseFeatureService courseFeatureService;

	@Autowired
	private CourseSpeechRequestService speechRequestService;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private SubscriberEmitterService subscriberEmmiter;

	@Autowired	
	private CourseMessengerFeatureSaveFeature messengerFeatureSaveFeature;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private MessengerFeatureUserRegistry messengerFeatureUserRegistry;

	@Autowired
	private MessageValidator messageValidator;


	@GetMapping("/user")
	public UserDto getUser(Authentication authentication) {
		Optional<User> userOpt = userService.findById(authentication.getName());
		User user = userOpt.get();
		UserDto userDto = UserDto.builder()
			.username(authentication.getName())
			.familyName(user.getFamilyName())
			.firstName(user.getFirstName()).build();

		return userDto;
	}


	@GetMapping("/courses")
	public List<CourseDto> getCourses(Authentication authentication) {
		List<CourseDto> courses = new ArrayList<>();

		courseService.findAllByUserId(authentication.getName()).forEach(course -> {
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

		speechRequestService.deleteById(speechRequest.getId());

		CourseSpeechEvent courseEvent = CourseSpeechEvent.builder()
			.requestId(BigInteger.valueOf(speechRequest.getRequestId()))
			.accepted(true)
			.build();

		var sEvent = ServerSentEvent.<CourseSpeechEvent>builder()
			.event("speech-state")
			.data(courseEvent)
			.build();

		subscriberEmmiter.send(speechRequest.getUserId(), new GenericMessage<>(sEvent));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/speech/reject/{requestId}")
	public ResponseEntity<Void> rejectSpeech(@PathVariable("requestId") long requestId) {
		Optional<CourseSpeechRequest> speechRequestOpt = speechRequestService.findByRequestId(requestId);

		if (speechRequestOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).build();
		}

		CourseSpeechRequest speechRequest = speechRequestOpt.get();

		speechRequestService.deleteById(speechRequest.getId());

		CourseSpeechEvent courseEvent = CourseSpeechEvent.builder()
			.requestId(BigInteger.valueOf(speechRequest.getRequestId()))
			.accepted(false)
			.build();

		var sEvent = ServerSentEvent.<CourseSpeechEvent>builder()
			.event("speech-state")
			.data(courseEvent)
			.build();

		subscriberEmmiter.send(speechRequest.getUserId(), new GenericMessage<>(sEvent));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/messenger/start/{courseId}")
	public ResponseEntity<String> startMessenger(@PathVariable("courseId") long courseId) {
		return startFeature(courseId, new CourseMessageFeature());
	}

	@PostMapping("/messenger/stop/{courseId}")
	public ResponseEntity<String> stopMessenger(@PathVariable("courseId") long courseId) {
		return stopFeature(courseId, CourseMessageFeature.class);
	}

	@GetMapping("/messenger/users/{courseId}")
	public Set<UserDto> getConnectedMessengerUsers(@PathVariable("courseId") long courseId, Authentication authentication) {
		courseFeatureService.findMessageByCourseId(courseId).orElseThrow(() -> new FeatureNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		Set<MessengerFeatureUser> connectedUsers = messengerFeatureUserRegistry.getUsers(courseId);


		Comparator<UserDto> userComparator = new Comparator<UserDto>() {
			@Override
			public int compare(UserDto arg0, UserDto arg1) {
				return arg0.getUsername().compareTo(arg1.getUsername());
			};
		};

		TreeSet<UserDto> sortedConnectedUsers = new TreeSet<>(userComparator);

		connectedUsers.forEach((user) -> {
			if (!user.getUsername().equals(details.getUsername())) {
				User u = userService.findById(user.getUsername()).get();
				UserDto userDto = new UserDto(u.getFirstName(), u.getFamilyName(), u.getUserId());
				sortedConnectedUsers.add(userDto);
			}
		});

		return sortedConnectedUsers;
	}

	@PostMapping("/quiz/start/{courseId}")
	public ResponseEntity<String> startQuiz(@PathVariable("courseId") long courseId, @RequestBody Quiz quiz) {
		CourseQuizFeature feature = new CourseQuizFeature();
		feature.setQuestion(StringUtils.cleanHtml(quiz.getQuestion()));
		feature.setType(quiz.getType());
		feature.setOptions(quiz.getOptions());

		return startFeature(courseId, feature);
	}

	@PostMapping("/quiz/stop/{courseId}")
	public ResponseEntity<String> stopQuiz(@PathVariable("courseId") long courseId) {
		return stopFeature(courseId, CourseQuizFeature.class);
	}

	ResponseEntity<String> startFeature(long courseId, CourseFeature feature) {
		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		CourseFeature courseFeature = course.getFeatures()
				.stream()
				.filter(s -> s.getClass().equals(feature.getClass()))
				.findFirst().orElse(null);

		if (isNull(courseFeature)) {
			feature.setCourse(course);
			feature.setFeatureId(Long.toString(new SecureRandom().nextLong()));

			course.getFeatures().add(feature);

			courseService.saveCourse(course);
			if (feature instanceof CourseMessageFeature) {
				messengerFeatureSaveFeature.addCourseHistory(courseId);
			}

			// Send feature state event.
			sendFeatureState(course.getId(), feature, true);
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
		}

		// Send feature state event.
		sendFeatureState(course.getId(), courseFeature, false);

		return ResponseEntity.status(HttpStatus.OK).body(courseFeature.getFeatureId());
	}

	void sendFeatureState(long courseId, CourseFeature feature, boolean started) {
		CourseFeatureEvent courseEvent = CourseFeatureEvent.builder()
			.courseId(courseId)
			.createdTimestamp(System.currentTimeMillis())
			.started(started)
			.build();

		var sEvent = ServerSentEvent.<CourseEvent>builder()
			.event(feature.getName() + "-state")
			.data(courseEvent)
			.build();

		subscriberEmmiter.send(new GenericMessage<>(sEvent));
	}

	@MessageMapping("/message/publisher/{courseId}")
    @SendTo("/topic/chat/{courseId}")
    public void sendMessage(@Payload String messageString, @DestinationVariable Long courseId, Authentication authentication) throws Exception {
		CourseMessageFeature feature = (CourseMessageFeature) courseFeatureService.findMessageByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		JsonNode jsonNode = this.objectMapper.readTree(messageString);
		String type = jsonNode.get("type").asText();
		WebMessage message;
		switch(type) {
			case "MessengerMessage":
				message = this.objectMapper.readValue(messageString, MessengerMessage.class);
				messengerFeatureSaveFeature.onFeatureMessage(courseId, message);
				simpMessagingTemplate.convertAndSend("/topic/chat/" + courseId, message, Map.of("payloadType", "MessengerMessage")); 
				break;
			case "MessengerReplyMessage":
				message = this.objectMapper.readValue(messageString, MessengerReplyMessage.class);
				messengerFeatureSaveFeature.onFeatureMessage(courseId, message);
				simpMessagingTemplate.convertAndSend("/topic/chat/" + courseId, message, Map.of("payloadType", "MessengerReplyMessage")); 
				break;
			case "MessengerDirectMessage":
				message = this.objectMapper.readValue(messageString, MessengerDirectMessage.class);
				messengerFeatureSaveFeature.onFeatureMessage(courseId, message);
				MessengerDirectMessage mdm = (MessengerDirectMessage) message;
				Set<String> stompDestinationUsernamesInUse = messengerFeatureUserRegistry.getUser(mdm.getMessageDestinationUsername()).getAddressesInUse();
				Set<String> stompUsernamesInUse = messengerFeatureUserRegistry.getUser(details.getUsername()).getAddressesInUse();
				ArrayList<Set<String>> sets = new ArrayList<>(Arrays.asList(stompDestinationUsernamesInUse, stompUsernamesInUse));
				for (Set<String> set : sets) {
					for (String userDestination : set) {
						simpMessagingTemplate.convertAndSendToUser(userDestination,"/queue/chat/" + courseId, message, Map.of("payloadType", "MessengerDirectMessage"));
					}
				}
				break;
			default:
				return; //To-DO give useful output
		}
    }
}
