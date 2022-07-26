package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.lecturestudio.core.recording.RecordedPage;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.service.CourseQuizResourceService;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.RoleService;
import org.lecturestudio.web.portal.service.UserService;
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
import org.lecturestudio.web.portal.model.dto.CourseStateDto;
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
	private CourseService courseService;

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private CourseFeatureService courseFeatureService;

	@Autowired
	private CourseSpeechRequestService speechRequestService;

	@Autowired
	private CourseQuizResourceService courseQuizResourceService;

	@Autowired
	private CourseMessengerFeatureSaveFeature messengerFeatureSaveFeature;

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

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
			message.setUserId(details.getUsername());

			simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/speech", message, Map.of("payloadType", message.getClass().getSimpleName()));

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
			message.setUserId(details.getUsername());

			simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/speech", message, Map.of("payloadType", message.getClass().getSimpleName()));
		}

		return response;
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

		if (! (response.getStatusCode().value() == HttpStatus.OK.value())) {
			throw new Exception(response.toString());
		}

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		String recipient = accessor.getFirstNativeHeader("recipient");

		if (recipient.equals("public")) {
			WebMessage forwardMessage = new MessengerMessage(payload, details.getUsername(), ZonedDateTime.now());
			forwardMessage.setFirstName(details.getFirstName());
			forwardMessage.setFamilyName(details.getFamilyName());

			messengerFeatureSaveFeature.onFeatureMessage(courseId, forwardMessage);

			simpMessagingTemplate.convertAndSend("/topic/chat/" + courseId, forwardMessage, Map.of("payloadType", forwardMessage.getClass().getSimpleName()));
		}
		else {
			CoursePrivilege requiredToSendToUserPrivilege = roleService
					.findByPrivilegeName("COURSE_MESSENGER_WRITE_DIRECT_PRIVILEGE")
					.orElseThrow(() -> new CoursePrivilegeNotFoundException());

			roleService.checkAuthorization(course, details, requiredToSendToUserPrivilege);

			User destUser = userService.findById(recipient).orElse(null);

			if (isNull(destUser)) {
				// User not found, abort.
				return;
			}

			MessengerDirectMessage forwardMessage = new MessengerDirectMessage(recipient, payload, details.getUsername(), ZonedDateTime.now());
			forwardMessage.setFirstName(details.getFirstName());
			forwardMessage.setFamilyName(details.getFamilyName());
			forwardMessage.setReply(false);

			messengerFeatureSaveFeature.onFeatureMessage(courseId, forwardMessage);

			// Send back to the sender.
			simpMessagingTemplate.convertAndSendToUser(details.getUsername(), "/queue/chat/" + courseId,
					forwardMessage, Map.of("payloadType", forwardMessage.getClass().getSimpleName()));

			// Send to the recipient.
			simpMessagingTemplate.convertAndSendToUser(recipient, "/queue/chat/" + courseId,
					forwardMessage, Map.of("payloadType", forwardMessage.getClass().getSimpleName()));
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

			simpMessagingTemplate.convertAndSend("/topic/quiz/" + courseId, qMessage, Map.of("payloadType", qMessage.getClass().getSimpleName()));
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

		simpMessagingTemplate.convertAndSend("/topic/course/all/stream", courseEvent);
		simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/stream", courseEvent);
	}
}
