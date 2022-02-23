package org.lecturestudio.web.portal.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.lecturestudio.core.recording.RecordedPage;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.service.CourseSpeechRequestService;
import org.lecturestudio.web.portal.service.FileStorageService;
import org.lecturestudio.web.portal.service.SubscriberEmitterService;
import org.lecturestudio.web.portal.validator.MessageValidator;
import org.lecturestudio.web.portal.validator.QuizAnswerValidator;
import org.lecturestudio.web.portal.validator.SpeechValidator;
import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.QuizAnswerMessage;
import org.lecturestudio.web.api.message.SpeechCancelMessage;
import org.lecturestudio.web.api.message.SpeechRequestMessage;
import org.lecturestudio.web.api.model.Message;
import org.lecturestudio.web.api.model.quiz.QuizAnswer;
import org.lecturestudio.web.api.stream.model.CourseFeatureResponse;
import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.exception.DocumentNotFoundException;
import org.lecturestudio.web.portal.exception.FeatureNotFoundException;
import org.lecturestudio.web.portal.model.CourseEvent;
import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStateDocument;
import org.lecturestudio.web.portal.model.CourseStateListener;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.dto.CourseStateDto;
import org.lecturestudio.web.portal.saml.LectUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink.OverflowStrategy;

@RestController
@RequestMapping("/course")
public class CourseSubscriberController {

	@Autowired
	private SubscriberEmitterService subscriberEmmiter;

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private CourseFeatureState courseFeatureState;

	@Autowired
	private CourseFeatureService courseFeatureService;

	@Autowired
	private CourseSpeechRequestService speechRequestService;

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
	public CourseStateDto getCourseState(@PathVariable("id") long id) {
		CourseState courseState = courseStates.getCourseState(id);

		if (isNull(courseState)) {
			throw new CourseNotFoundException();
		}

		return CourseStateDto.builder()
			.avtiveDocument(courseState.getActiveDocument())
			.documentMap(courseState.getAllCourseStateDocuments())
			.build();
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

	@GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<CourseEvent>> getEvents(Authentication authentication) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		final String userId = details.getUsername();

		return Flux.create(sink -> {
			MessageHandler handler = message -> sink.next(ServerSentEvent.class.cast(message.getPayload()));
			sink.onCancel(() -> {
				subscriberEmmiter.unsubscribe(userId, handler);
			});

			subscriberEmmiter.subscribe(userId, handler);
		}, OverflowStrategy.LATEST);
	}

	@PostMapping(value = "/speech/{courseId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> postSpeech(@PathVariable("courseId") long courseId,
			Authentication authentication) {
		CourseState courseState = courseStates.getCourseState(courseId);

		if (isNull(courseState)) {
			throw new CourseNotFoundException();
		}

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		// Validate input.
		ResponseEntity<CourseFeatureResponse> response = speechValidator.validate(courseId);

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			// speechValidator.registerRequest(courseId);

			CourseSpeechRequest speechRequest = CourseSpeechRequest.builder()
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

			courseState.postSpeechMessage(courseId, message);

			return ResponseEntity.ok().body(speechRequest.getRequestId());
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@DeleteMapping("/speech/{courseId}/{requestId}")
	public ResponseEntity<CourseFeatureResponse> cancelSpeech(@PathVariable("courseId") long courseId,
			@PathVariable("requestId") long requestId, Authentication authentication) {
		CourseState courseState = courseStates.getCourseState(courseId);

		if (isNull(courseState)) {
			throw new CourseNotFoundException();
		}

		LectUserDetails details = (LectUserDetails) authentication.getDetails();

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

	@PostMapping("/quiz/post/{courseId}")
	public ResponseEntity<CourseFeatureResponse> postQuizAnswer(@PathVariable("courseId") long courseId,
			@RequestBody QuizAnswer quizAnswer, Authentication authentication, HttpServletRequest request) {

		final CourseQuizFeature feature = (CourseQuizFeature) courseFeatureService.findQuizByCourseId(courseId)
				.orElseThrow(() -> new FeatureNotFoundException());

		final LectUserDetails details = (LectUserDetails) authentication.getDetails();
		final String userName = details.getUsername();

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

	private void sendCourseEvent(CourseState state, long courseId, boolean started) {
		CourseEvent courseEvent = CourseEvent.builder()
			.courseId(courseId)
			.createdTimestamp(nonNull(state) ? state.getCreatedTimestamp() : null)
			.started(started)
			.build();

		var sEvent = ServerSentEvent.<CourseEvent>builder()
			.event("stream-state")
			.data(courseEvent)
			.build();

		subscriberEmmiter.send(new GenericMessage<>(sEvent));
	}
}
