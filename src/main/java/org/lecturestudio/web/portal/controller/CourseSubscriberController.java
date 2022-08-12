package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.lecturestudio.core.recording.RecordedPage;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.service.CourseParticipantService;
import org.lecturestudio.web.portal.service.CoursePresenceService;
import org.lecturestudio.web.portal.service.CourseQuizResourceService;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.util.SimpEmitter;
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
import org.lecturestudio.web.portal.exception.DocumentNotFoundException;
import org.lecturestudio.web.portal.exception.FeatureNotFoundException;
import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseEvent;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseQuizResource;
import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStateDocument;
import org.lecturestudio.web.portal.model.CourseStateListener;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.dto.CourseMessengerHistoryDto;
import org.lecturestudio.web.portal.model.dto.CoursePrivilegeDto;
import org.lecturestudio.web.portal.model.dto.CourseStateDto;
import org.lecturestudio.web.portal.model.dto.UserDto;
import org.lecturestudio.web.portal.property.SimpProperties;
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
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
	private CourseParticipantService participantService;

	@Autowired
	private CoursePresenceService presenceService;

	@Autowired
	private UserService userService;

	@Autowired
	private SimpProperties simpProperties;

	@Autowired
	private SimpEmitter simpEmitter;

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

	@GetMapping("/state/{id}")
	public CourseStateDto getCourseState(@PathVariable("id") long id, Authentication authentication) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		Course course = courseService.findById(id)
				.orElseThrow(() -> new CourseNotFoundException());

		boolean isProtected = nonNull(course.getPasscode()) && !course.getPasscode().isEmpty();

		CourseMessageFeature messageFeature = null;
		CourseQuizFeature quizFeature = null;

		CourseState courseState = courseStates.getCourseState(id);

		Set<CoursePrivilegeDto> userPrivileges = new HashSet<>();

		for (Privilege privilege : courseService.getUserPrivileges(id, details.getUsername())) {
			userPrivileges.add(CoursePrivilegeDto.builder()
					.name(privilege.getName())
					.descriptionKey(privilege.getDescriptionKey())
					.build());
		}

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
			.isProtected(isProtected)
			.userPrivileges(userPrivileges);

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

			if (!participants.contains(participant)) {
				participants.add(participant);
			}
		});

		return participants;
	}

	@GetMapping("/chat/history/{courseId}")
	public CourseMessengerHistoryDto getChatHistory(@PathVariable("courseId") Long courseId, Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "CHAT_READ")) {
			return new CourseMessengerHistoryDto(List.of());
		}

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		User user = userService.findById(details.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		return new CourseMessengerHistoryDto(messengerFeatureSaveFeature.getMessengerHistoryOfCourse(courseId, user));
	}

	@PostMapping(value = "/speech/{courseId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> postSpeech(@PathVariable("courseId") long courseId,
			Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "SPEECH")) {
			throw new UnauthorizedException();
		}

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = speechValidator.validate(courseId);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			LectUserDetails details = (LectUserDetails) authentication.getDetails();

			// speechValidator.registerRequest(courseId);

			CourseSpeechRequest speechRequest = CourseSpeechRequest.builder()
				.courseId(courseId)
				.requestId(new SecureRandom().nextLong())
				.userId(details.getUsername())
				.build();

			speechRequestService.saveRequest(speechRequest);

			// Notify course organisators.
			SpeechRequestMessage message = new SpeechRequestMessage();
			message.setRequestId(speechRequest.getRequestId());
			message.setDate(ZonedDateTime.now());
			message.setFirstName(details.getFirstName());
			message.setFamilyName(details.getFamilyName());
			message.setUserId(details.getUsername());

			for (String userId : courseService.getOrganisators(courseId)) {
				simpEmitter.emmitEventToUser(courseId, simpProperties.getEvents().getSpeech(), message, userId);
			}

			return ResponseEntity.ok().body(speechRequest.getRequestId());
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@DeleteMapping("/speech/{courseId}/{requestId}")
	public ResponseEntity<CourseFeatureResponse> cancelSpeech(@PathVariable("courseId") long courseId,
			@PathVariable("requestId") long requestId, Authentication authentication) {
		if (!courseService.isAuthorized(courseId, authentication, "SPEECH")) {
			throw new UnauthorizedException();
		}

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

			LectUserDetails details = (LectUserDetails) authentication.getDetails();

			CourseSpeechRequest speechRequest = speechRequestOpt.get();

			speechRequestService.deleteById(speechRequest.getId());

			// Notify course organisators.
			SpeechCancelMessage message = new SpeechCancelMessage();
			message.setRequestId(speechRequest.getRequestId());
			message.setDate(ZonedDateTime.now());
			message.setFirstName(details.getFirstName());
			message.setFamilyName(details.getFamilyName());
			message.setUserId(details.getUsername());

			for (String userId : courseService.getOrganisators(courseId)) {
				simpEmitter.emmitEventToUser(courseId, simpProperties.getEvents().getSpeech(), message, userId);
			}
		}

		return response;
	}

	@MessageMapping("/message/{courseId}")
    @SendTo("/topic/course/{courseId}/chat")
    public void sendMessage(@Payload org.springframework.messaging.Message<Message> message, @DestinationVariable Long courseId, Authentication authentication) throws Exception {
		if (!courseService.isAuthorized(courseId, authentication, "CHAT_WRITE")) {
			throw new UnauthorizedException();
		}

		CourseMessageFeature feature = (CourseMessageFeature) courseFeatureService.findMessageByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		Message payload = message.getPayload();

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = messageValidator.validate(feature, payload);

		if (!(response.getStatusCode().value() == HttpStatus.OK.value())) {
			throw new Exception(response.toString());
		}

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		String recipient = accessor.getFirstNativeHeader("recipient");

		if (recipient.equals("public")) {
			WebMessage chatMessage = new MessengerMessage(payload, details.getUsername(), ZonedDateTime.now());
			chatMessage.setFirstName(details.getFirstName());
			chatMessage.setFamilyName(details.getFamilyName());

			messengerFeatureSaveFeature.onFeatureMessage(courseId, chatMessage);

			simpEmitter.emmitChatMessage(courseId, chatMessage);
		}
		else if (recipient.equals("organisers")) {
			if (!courseService.isAuthorized(courseId, authentication, "CHAT_WRITE_TO_ORGANISATOR")) {
				throw new UnauthorizedException();
			}

			MessengerDirectMessage chatMessage = new MessengerDirectMessage(recipient);
			chatMessage.setUserId(details.getUsername());
			chatMessage.setFirstName(details.getFirstName());
			chatMessage.setFamilyName(details.getFamilyName());
			chatMessage.setMessage(payload);
			chatMessage.setDate(ZonedDateTime.now());
			chatMessage.setReply(false);

			messengerFeatureSaveFeature.onFeatureMessage(courseId, chatMessage);

			// Send back to the sender.
			simpEmitter.emmitChatMessageToUser(courseId, chatMessage, details.getUsername());

			// Send to all (co-)organisators.
			for (String userId : courseService.getOrganisators(courseId)) {
				chatMessage.setRecipient(userId);

				simpEmitter.emmitChatMessageToUser(courseId, chatMessage, userId);
			}
		}
		else {
			if (!courseService.isAuthorized(courseId, authentication, "CHAT_WRITE_PRIVATELY")) {
				throw new UnauthorizedException();
			}

			User destUser = userService.findById(recipient).orElse(null);

			if (isNull(destUser)) {
				// User not found, abort.
				return;
			}

			MessengerDirectMessage chatMessage = new MessengerDirectMessage(recipient);
			chatMessage.setUserId(details.getUsername());
			chatMessage.setFirstName(details.getFirstName());
			chatMessage.setFamilyName(details.getFamilyName());
			chatMessage.setMessage(payload);
			chatMessage.setDate(ZonedDateTime.now());
			chatMessage.setReply(false);

			messengerFeatureSaveFeature.onFeatureMessage(courseId, chatMessage);

			// Send back to the sender.
			simpEmitter.emmitChatMessageToUser(courseId, chatMessage, details.getUsername());

			// Send to the recipient.
			simpEmitter.emmitChatMessageToUser(courseId, chatMessage, recipient);
		}
    }

	@PostMapping("/quiz/post/{courseId}")
	public ResponseEntity<CourseFeatureResponse> postQuizAnswer(@PathVariable("courseId") long courseId,
			@RequestBody QuizAnswer quizAnswer, Authentication authentication, HttpServletRequest request) {
		if (!courseService.isAuthorized(courseId, authentication, "QUIZ_PARTICIPATION")) {
			throw new UnauthorizedException();
		}

		final CourseQuizFeature feature = (CourseQuizFeature) courseFeatureService.findQuizByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		final LectUserDetails details = (LectUserDetails) authentication.getDetails();
		final String userName = details.getUsername();

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = quizAnswerValidator.validate(userName, feature, quizAnswer);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			feature.getUsers().add(userName);

			courseFeatureService.save(feature);

			QuizAnswerMessage qMessage = new QuizAnswerMessage(quizAnswer, "", ZonedDateTime.now());

			for (String userId : courseService.getOrganisators(courseId)) {
				simpEmitter.emmitEventToUser(courseId, simpProperties.getEvents().getQuiz(), qMessage, userId);
			}
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
		exc.printStackTrace();
	}

	private void sendCourseEvent(CourseState state, long courseId, boolean started) {
		CourseEvent courseEvent = CourseEvent.builder()
			.courseId(courseId)
			.createdTimestamp(nonNull(state) ? state.getCreatedTimestamp() : null)
			.started(started)
			.build();

		simpEmitter.emmitEventAndAll(courseId, simpProperties.getEvents().getStream(), courseEvent);
	}
}
