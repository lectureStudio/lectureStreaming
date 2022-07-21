package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.lecturestudio.core.recording.RecordedPage;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.service.CourseQuizResourceService;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry;
import org.lecturestudio.web.portal.service.RoleService;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry.MessengerFeatureUser;
import org.lecturestudio.web.portal.validator.MessageValidator;
import org.lecturestudio.web.portal.validator.QuizAnswerValidator;
import org.lecturestudio.web.portal.validator.SpeechValidator;
import org.lecturestudio.web.api.message.MessengerDirectMessage;
import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.QuizAnswerMessage;
import org.lecturestudio.web.api.message.SpeechCancelMessage;
import org.lecturestudio.web.api.message.SpeechRequestMessage;
import org.lecturestudio.web.api.message.WebMessage;
import org.lecturestudio.web.api.model.Message;
import org.lecturestudio.web.api.model.quiz.QuizAnswer;
import org.lecturestudio.web.api.stream.model.CourseFeatureResponse;
import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.exception.CoursePrivilegeNotFoundException;
import org.lecturestudio.web.portal.exception.DocumentNotFoundException;
import org.lecturestudio.web.portal.exception.FeatureNotFoundException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseEvent;
import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseQuizResource;
import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStateDocument;
import org.lecturestudio.web.portal.model.CourseStateListener;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.dto.CourseMessengerConnectedUsersDto;
import org.lecturestudio.web.portal.model.dto.CourseMessengerHistoryDto;
import org.lecturestudio.web.portal.model.dto.CourseStateDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.saml.LectUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/course")
public class CourseSubscriberController {

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private CourseMessengerFeatureSaveFeature messengerFeatureSaveFeature;

	@Autowired
	private CourseService courseService;

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private CourseFeatureState courseFeatureState;

	@Autowired
	private CourseFeatureService courseFeatureService;

	@Autowired
	private CourseSpeechRequestService speechRequestService;

	@Autowired
	private CourseQuizResourceService courseQuizResourceService;

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private MessengerFeatureUserRegistry messengerFeatureUserRegistry;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private SpeechValidator speechValidator;

	@Autowired
	private MessageValidator messageValidator;

	@Autowired
	private QuizAnswerValidator quizAnswerValidator;


	@PostConstruct
	private void postConstruct() {
		courseStates.addCourseStateListener(new CourseStateListener() {

			@Override
			public void courseStarted(long courseId, CourseState state) {
				sendCourseEvent(state, courseId, true);
			}

			@Override
			public void courseEnded(long courseId, CourseState state) {
				sendCourseEvent(state, courseId, false);
			}

		});
	}

	@GetMapping("/privileges/{id}/check/{privilege}")
	public ResponseEntity<String> isAuthorized(@PathVariable("id") long courseId, @PathVariable("privilege") String privilege, Authentication authentication) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		Course course = courseService.findById(courseId)
			.orElseThrow(() -> new CourseNotFoundException());

		CoursePrivilege coursePrivilege = roleService.findByPrivilegeName(privilege)
			.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		return roleService.isAuthorized(course, details, coursePrivilege) ? ResponseEntity.status(HttpStatus.OK).build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	@GetMapping("/state/{id}")
	public CourseStateDto getCourseState(@PathVariable("id") long id, Authentication authentication) {
		Course course = courseService.findById(id)
				.orElseThrow(() -> new CourseNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		boolean isProtected = nonNull(course.getPasscode()) && !course.getPasscode().isEmpty();

		CourseMessageFeature messageFeature = null;
		CourseQuizFeature quizFeature = null;
		
		CourseState courseState = courseStates.getCourseState(id);

		for (var feature : course.getFeatures()) {
			if (feature instanceof CourseMessageFeature) {
				messageFeature = new CourseMessageFeature();
				messageFeature.setFeatureId(feature.getFeatureId());
			}
			else if (feature instanceof CourseQuizFeature) {
				CourseQuizFeature qFeature = (CourseQuizFeature) feature;

				quizFeature = new CourseQuizFeature();
				quizFeature.setFeatureId(qFeature.getFeatureId());
				quizFeature.setType(qFeature.getType());
				quizFeature.setQuestion(qFeature.getQuestion().replace("&#xa0;"," "));
				quizFeature.setOptions(qFeature.getOptions());
			}
		}

		var builder = CourseStateDto.builder()
				.courseId(id)
				.userId(details.getUsername())
				.title(course.getTitle())
				.description(course.getDescription())
				.messageFeature(messageFeature)
				.quizFeature(quizFeature)
				.isProtected(isProtected);

		if (nonNull(courseState)) {
			builder
				.timeStarted(nonNull(courseState) ? courseState.getCreatedTimestamp() : null)
				.isRecorded(nonNull(courseState) ? courseState.getRecordedState() : false)
				.activeDocument(courseState.getActiveDocument())
				.documentMap(courseState.getAllCourseStateDocuments());
		}

		return builder.build();
	}

	@GetMapping("/state/{id}/pages/{docId}")
	public ResponseEntity<Resource> getPageState(@PathVariable("id") long id, @PathVariable("docId") Long docId) {
		CourseState courseState = courseStates.getCourseState(id);

		if (isNull(courseState)) {
			throw new CourseNotFoundException();
		}

		CourseStateDocument stateDoc = courseState.getCourseStateDocument(docId);

		if (isNull(stateDoc)) {
			throw new DocumentNotFoundException();
		}

		ByteArrayOutputStream pageStream = new ByteArrayOutputStream();

		for (var statePage : stateDoc.getPages().values()) {
			RecordedPage recordedPage = new RecordedPage();
			recordedPage.setNumber(statePage.getPageNumber());
			recordedPage.getPlaybackActions().addAll(statePage.getActions());

			try {
				pageStream.write(recordedPage.toByteArray());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		ByteArrayResource resource = new ByteArrayResource(pageStream.toByteArray());

		return ResponseEntity.ok()
			.contentLength(resource.contentLength())
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(resource);
	}

	@PostMapping(value = "/speech/{courseId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> postSpeech(@PathVariable("courseId") long courseId,
			Authentication authentication) {
		CoursePrivilege requiredPrivilege = roleService.findByPrivilegeName("COURSE_RAISE_HAND_PRIVILEGE")
				.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		roleService.checkAuthorization(course, details, requiredPrivilege);

		CourseState courseState = courseStates.getCourseState(courseId);

		if (isNull(courseState)) {
			throw new CourseNotFoundException();
		}

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = speechValidator.validate(courseId);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			// speechValidator.registerRequest(courseId);

			CourseSpeechRequest speechRequest = CourseSpeechRequest.builder()
				.courseId(courseId)
				.requestId(new SecureRandom().nextLong())
				.userId(details.getUsername())
				.build();

			speechRequestService.saveRequest(speechRequest);

			// Notify service provider endpoint.
			SpeechRequestMessage message = new SpeechRequestMessage();
			message.setRequestId(speechRequest.getRequestId());
			message.setDate(ZonedDateTime.now());
			message.setFirstName(details.getFirstName());
			message.setFamilyName(details.getFamilyName());
			message.setRemoteAddress(details.getUsername());

			courseState.postSpeechMessage(courseId, message);

			return ResponseEntity.ok().body(speechRequest.getRequestId());
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@DeleteMapping("/speech/{courseId}/{requestId}")
	public ResponseEntity<CourseFeatureResponse> cancelSpeech(@PathVariable("courseId") long courseId,
			@PathVariable("requestId") long requestId, Authentication authentication) {
		CoursePrivilege requiredPrivilege = roleService.findByPrivilegeName("COURSE_RAISE_HAND_PRIVILEGE")
				.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		roleService.checkAuthorization(course, details, requiredPrivilege);

		CourseState courseState = courseStates.getCourseState(courseId);

		if (isNull(courseState)) {
			throw new CourseNotFoundException();
		}

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = speechValidator.validate(courseId);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			// speechValidator.registerRequest(courseId);

			Optional<CourseSpeechRequest> speechRequestOpt = speechRequestService
					.findByRequestId(requestId);

			if (speechRequestOpt.isEmpty()) {
				return response;
			}

			CourseSpeechRequest speechRequest = speechRequestOpt.get();

			speechRequestService.deleteById(speechRequest.getId());

			// Notify service provider endpoint.
			SpeechCancelMessage message = new SpeechCancelMessage();
			message.setRequestId(speechRequest.getRequestId());
			message.setDate(ZonedDateTime.now());
			message.setFirstName(details.getFirstName());
			message.setFamilyName(details.getFamilyName());
			message.setRemoteAddress(details.getUsername());

			courseState.postSpeechMessage(courseId, message);
		}

		return response;
	}

	@PostMapping("/message/post/{courseId}")
	public ResponseEntity<CourseFeatureResponse> postMessage(@PathVariable("courseId") long courseId,
			@RequestBody Message message, Authentication authentication, HttpServletRequest request) {
		CourseMessageFeature feature = (CourseMessageFeature) courseFeatureService.findMessageByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = messageValidator.validate(feature, message);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			MessengerMessage mMessage = new MessengerMessage(message, request.getRemoteAddr(), ZonedDateTime.now());
			mMessage.setFirstName(details.getFirstName());
			mMessage.setFamilyName(details.getFamilyName());

			// Notify service provider endpoint.
			courseFeatureState.postCourseFeatureMessage(courseId, mMessage);
		}

		return response;
	}

	@GetMapping("/messenger/history/{courseId}")
	public CourseMessengerHistoryDto getMessengerHistoryOfCourse(@PathVariable("courseId") long courseId, Authentication authentication) {
		Course course = courseService.findById(courseId)
			.orElseThrow(() -> new CourseNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		CoursePrivilege requiredToReadPrivilege = roleService.findByPrivilegeName("COURSE_MESSENGER_READ_PRIVILEGE")
			.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		roleService.checkAuthorization(course, details, requiredToReadPrivilege);

		User user = userService.findById(details.getUsername()).get();

		return new CourseMessengerHistoryDto(messengerFeatureSaveFeature.getMessengerHistoryOfCourse(courseId, user));
	}

	@GetMapping("/messenger/users/{courseId}")
	public CourseMessengerConnectedUsersDto getConnectedMessengerUsers(@PathVariable("courseId") long courseId, Authentication authentication) {
		courseFeatureService.findMessageByCourseId(courseId).orElseThrow(() -> new FeatureNotFoundException());

		Course course = courseService.findById(courseId)
			.orElseThrow(() -> new CourseNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		CoursePrivilege requiredPrivilege = roleService.findByPrivilegeName("COURSE_MESSENGER_WRITE_DIRECT_PRIVILEGE")
			.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		roleService.checkAuthorization(course, details, requiredPrivilege);

		CourseMessengerConnectedUsersDto connectedUsersDto = new CourseMessengerConnectedUsersDto();

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
				UserDto userDto = new UserDto(u.getFirstName(), u.getFamilyName(), u.getAnonymousUserId().toString());
				sortedConnectedUsers.add(userDto);
			}
		});

		connectedUsersDto.setConnectedUsers(sortedConnectedUsers);

		return connectedUsersDto;
	}

	@MessageMapping("/message/{courseId}")
    @SendTo("/topic/chat/{courseId}")
    public void sendMessage(@Payload org.springframework.messaging.Message<Message> message, @DestinationVariable Long courseId, Authentication authentication) throws Exception {
		CourseMessageFeature feature = (CourseMessageFeature) courseFeatureService.findMessageByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		Course course = courseService.findById(courseId)
			.orElseThrow(() -> new CourseNotFoundException());

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		CoursePrivilege requiredToSendPrivilege = roleService.findByPrivilegeName("COURSE_MESSENGER_WRITE_PRIVILEGE")
			.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		roleService.checkAuthorization(course, details, requiredToSendPrivilege);

		Message payload = message.getPayload();

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = messageValidator.validate(feature, payload);

		System.out.println(payload.getServiceId() + ": " + payload.getText());
		System.out.println(response.getBody().statusCode + ": " + response.getBody().statusMessage);

		if (! (response.getStatusCode().value() == HttpStatus.OK.value())) {
			throw new Exception(response.toString());
		}

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		String messageType = accessor.getNativeHeader("messageType").get(0);

		WebMessage forwardMessage = null;

		switch(messageType) {
			case "user":
			case "lecturer":
				String messageDestinationUsername = "";
				if (messageType.equals("user")) {
					CoursePrivilege requiredToSendToUserPrivilege = roleService.findByPrivilegeName("COURSE_MESSENGER_WRITE_DIRECT_PRIVILEGE")
						.orElseThrow(() -> new CoursePrivilegeNotFoundException());

					roleService.checkAuthorization(course, details, requiredToSendToUserPrivilege);

					String anonymousMessageDestinationUsername = accessor.getNativeHeader("username").get(0);
					Optional<User> optDestinationUser = userService.findByAnonymousId(UUID.fromString(anonymousMessageDestinationUsername));
					if (optDestinationUser.isPresent()) {
						User destinationUser = optDestinationUser.get();
						messageDestinationUsername = destinationUser.getUserId();
					}
				}
				else {
					CoursePrivilege requiredToSendToLecturerPrivilege = roleService.findByPrivilegeName("COURSE_MESSENGER_WRITE_LECTURER_PRIVILEGE")
						.orElseThrow(() -> new CoursePrivilegeNotFoundException());

					roleService.checkAuthorization(course, details, requiredToSendToLecturerPrivilege);
					messageDestinationUsername = feature.getInitiator().getUserId();
				}

				forwardMessage = new MessengerDirectMessage(messageDestinationUsername, payload, details.getUsername(), ZonedDateTime.now());
				forwardMessage.setFirstName(details.getFirstName());
				forwardMessage.setFamilyName(details.getFamilyName());

				courseFeatureState.postCourseFeatureMessage(courseId, forwardMessage);

				MessengerFeatureUser user = messengerFeatureUserRegistry.getUser(details.getUsername());
				MessengerFeatureUser destinationUser = messengerFeatureUserRegistry.getUser(messageDestinationUsername);

				ArrayList<Set<String>> sets = new ArrayList<>();

				if (nonNull(destinationUser)) {
					sets.add(destinationUser.getAddressesInUse());
				}
				if (nonNull(user)) {
					sets.add(user.getAddressesInUse());
				}
				if (messageType.equals("lecturer")) {
					sets.add(Collections.singleton(messageDestinationUsername));
				}
				for (Set<String> set : sets) {
					for (String userDestination : set) {
						simpMessagingTemplate.convertAndSendToUser(userDestination,"/queue/chat/" + courseId, forwardMessage, Map.of("payloadType", "MessengerDirectMessage"));
					}
				}
				break;
			case "public":
				forwardMessage = new MessengerMessage(payload, details.getUsername(), ZonedDateTime.now());
				forwardMessage.setFirstName(details.getFirstName());
				forwardMessage.setFamilyName(details.getFamilyName());

				courseFeatureState.postCourseFeatureMessage(courseId, forwardMessage);

				simpMessagingTemplate.convertAndSend("/topic/chat/" + courseId, forwardMessage, Map.of("payloadType", "MessengerMessage"));
				break;
		}
    }

	@PostMapping("/quiz/post/{courseId}")
	public ResponseEntity<CourseFeatureResponse> postQuizAnswer(@PathVariable("courseId") long courseId,
			@RequestBody QuizAnswer quizAnswer, Authentication authentication, HttpServletRequest request) {
		Course course = courseService.findById(courseId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid course Id: " + courseId));

		final CourseQuizFeature feature = (CourseQuizFeature) courseFeatureService.findQuizByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		final LectUserDetails details = (LectUserDetails) authentication.getDetails();
		final String userName = details.getUsername();

		CoursePrivilege requiredPrivilege = roleService.findByPrivilegeName("COURSE_QUIZ_PRIVILEGE")
				.orElseThrow(() -> new CoursePrivilegeNotFoundException());

		roleService.checkAuthorization(course, details, requiredPrivilege);

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = quizAnswerValidator.validate(userName, feature, quizAnswer);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			feature.getUsers().add(userName);

			courseFeatureService.save(feature);

			QuizAnswerMessage qMessage = new QuizAnswerMessage(quizAnswer, request.getRemoteAddr(), ZonedDateTime.now());
			qMessage.setFirstName(details.getFirstName());
			qMessage.setFamilyName(details.getFamilyName());

			// Notify service provider endpoint.
			courseFeatureState.postCourseFeatureMessage(courseId, qMessage);
		}

		return response;
	}

	@GetMapping("/{courseId}/quiz/resource/{fileName:.+}")
	public ResponseEntity<Resource> getQuizResource(@PathVariable("courseId") long courseId, @PathVariable String fileName) {
		CourseQuizResource resource = courseQuizResourceService.findByCourseIdAndName(courseId, fileName)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(resource.getType()))
			.body(new ByteArrayResource(resource.getContent()));
	}

	@GetMapping("/file/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
		Resource file = fileStorageService.load(fileName);

		// Try to determine file's content type.
		String contentType = null;

		try {
			contentType = request.getServletContext().getMimeType(file.getFile().getAbsolutePath());
		}
		catch (IOException ex) {
			// Fallback to the default content type.
			contentType = "application/octet-stream";
		}

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
			.body(file);
	}

	@MessageExceptionHandler
	public void messengerMessageError(Exception exc) {
		System.out.println(exc.getMessage());
	}

	private void sendCourseEvent(CourseState state, long courseId, boolean started) {
		CourseEvent courseEvent = CourseEvent.builder()
			.courseId(courseId)
			.createdTimestamp(nonNull(state) ? state.getCreatedTimestamp() : null)
			.started(started)
			.build();

		simpMessagingTemplate.convertAndSend("/topic/course-state/all/stream", courseEvent);
		simpMessagingTemplate.convertAndSend("/topic/course-state/" + courseId + "/stream", courseEvent);
	}
}
