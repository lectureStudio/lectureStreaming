package org.lecturestudio.web.portal.websocket;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.core.input.KeyEvent;
import org.lecturestudio.web.api.message.CourseParticipantMessage;
import org.lecturestudio.web.api.message.SpeechBaseMessage;
import org.lecturestudio.web.api.stream.action.StreamAction;
import org.lecturestudio.web.api.stream.action.StreamActionFactory;
import org.lecturestudio.web.api.stream.action.StreamActionType;
import org.lecturestudio.web.api.stream.action.StreamDocumentAction;
import org.lecturestudio.web.api.stream.action.StreamInitAction;
import org.lecturestudio.web.api.stream.action.StreamPageAction;
import org.lecturestudio.web.api.stream.action.StreamPageActionsAction;
import org.lecturestudio.web.api.stream.action.StreamPagePlaybackAction;
import org.lecturestudio.web.api.stream.action.StreamStartAction;
import org.lecturestudio.web.portal.model.CourseConnectionRequest;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStateDocument;
import org.lecturestudio.web.portal.model.CourseStatePage;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.service.CourseConnectionRequestService;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class CourseStateWebSocketHandler extends BinaryWebSocketHandler {

	private final Map<WebSocketSession, Long> sessions = new ConcurrentHashMap<>();

	private final Map<WebSocketSession, List<ByteBuffer>> sessionBufferMap = new ConcurrentHashMap<>();

	private final Map<Long, CourseState> initStates = new ConcurrentHashMap<>();

	private final CourseStates courseStates;

	private final ObjectMapper objectMapper;

	private final CourseConnectionRequestService connectionRequestService;


	public CourseStateWebSocketHandler(CourseStates courseStates, ObjectMapper objectMapper, CourseConnectionRequestService connectionRequestService) {
		this.courseStates = courseStates;
		this.objectMapper = objectMapper;
		this.connectionRequestService = connectionRequestService;
	}

	@Override
	public boolean supportsPartialMessages() {
		return true;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// No-op.
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// Clean-up state.
		sessionBufferMap.remove(session);

		Long courseId = sessions.remove(session);

		initStates.remove(courseId);
		courseStates.removeCourseState(courseId);
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
		ByteBuffer buffer = message.getPayload();
		List<ByteBuffer> buffers = sessionBufferMap.get(session);

		if (message.isLast()) {
			if (isNull(buffers)) {
				parse(session, buffer);
			}
			else {
				buffers.add(buffer);

				int size = 0;

				for (ByteBuffer b : buffers) {
					size += b.limit();
				}

				ByteBuffer wrappedBuffer = ByteBuffer.allocate(size);

				for (ByteBuffer b : buffers) {
					wrappedBuffer.put(b);
				}

				wrappedBuffer.clear();

				while (wrappedBuffer.position() < wrappedBuffer.capacity()) {
					parse(session, wrappedBuffer);
				}

				sessionBufferMap.remove(session);
			}
		}
		else {
			if (isNull(buffers)) {
				buffers = new CopyOnWriteArrayList<>();
				sessionBufferMap.put(session, buffers);
			}

			buffers.add(buffer);
		}
	}

	private void parse(WebSocketSession session, ByteBuffer buffer) throws IOException {
		StreamAction action = parseActionBuffer(buffer);

		// System.out.println(action.getType());
		new KeyEvent(0);

		if (action.getType() == StreamActionType.STREAM_INIT) {
			sessionInit(session, (StreamInitAction) action);
		}
		else if (action.getType() == StreamActionType.STREAM_START) {
			sessionStart((StreamStartAction) action);
		}
		else {
			Long courseId = sessions.get(session);

			if (isNull(courseId)) {
				throw new IllegalStateException("Session not bound to a course");
			}

			CourseState courseState = courseStates.getCourseState(courseId);

			if (isNull(courseState)) {
				// When we are in the course init state.
				courseState = initStates.get(courseId);
			}

			switch (action.getType()) {
				case STREAM_DOCUMENT_CREATED:
					updateDocumentState(courseState, (StreamDocumentAction) action);
					break;

				case STREAM_DOCUMENT_CLOSED:
					
					break;

				case STREAM_DOCUMENT_SELECTED:
					updateActiveDocumentState(courseState, (StreamDocumentAction) action);
					break;

				case STREAM_PAGE_CREATED:
					updateCreatedPageState(courseState, (StreamPageAction) action);
					break;

				case STREAM_PAGE_DELETED:
					updateDeletedPageState(courseState, (StreamPageAction) action);
					break;

				case STREAM_PAGE_SELECTED:
					updateActivePageState(courseState, (StreamPageAction) action);
					break;

				case STREAM_PAGE_ACTION:
					updatePageActionState(courseState, (StreamPagePlaybackAction) action);
					break;

				case STREAM_PAGE_ACTIONS:
					updatePageActionsState(courseState, (StreamPageActionsAction) action);
					break;

				default:
					throw new InvalidObjectException("Action with type " + action.getType() + " does not exist");
			}
		}
	}

	private void onSpeechMessage(Long courseId, SpeechBaseMessage message) {
		WebSocketSession session = sessions.entrySet().stream()
			.filter(entry -> entry.getValue() == courseId)
			.findFirst()
			.map(Map.Entry::getKey)
			.orElse(null);

		if (isNull(session)) {
			throw new RuntimeException("Course not available");
		}

		try {
			session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onParticipantMessage(Long courseId, CourseParticipantMessage message) {
		WebSocketSession session = sessions.entrySet().stream()
			.filter(entry -> entry.getValue() == courseId)
			.findFirst()
			.map(Map.Entry::getKey)
			.orElse(null);

		if (isNull(session)) {
			throw new RuntimeException("Course not available");
		}

		try {
			String mess = objectMapper.writeValueAsString(message);
			session.sendMessage(new TextMessage(mess));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sessionInit(WebSocketSession session, StreamInitAction initAction) {
		long courseId = initAction.getCourseId();

		// Bind session to the course ID.
		sessions.put(session, courseId);

		// Bind course state to the course ID.
		initStates.put(courseId, new CourseState(courseId, this::onSpeechMessage, this::onParticipantMessage));
	}

	private void sessionStart(StreamStartAction startAction) {
		long courseId = startAction.getCourseId();
		CourseState state = initStates.remove(courseId);

		courseStates.setCourseState(courseId, state);

		HashSet<String> considered = new HashSet<>();
		for (CourseConnectionRequest connectionRequest : this.connectionRequestService.getAllByCourseId(courseId)) {
			if (! considered.contains(connectionRequest.getUserId())) {
				CourseParticipantMessage message = new CourseParticipantMessage();
				message.setConnected(true);
				message.setUsername(connectionRequest.getUserId());
				message.setFirstName(connectionRequest.getFirstName());
				message.setFamilyName(connectionRequest.getFamilyName());
				
				state.postParticipantMessage(courseId, message);
				considered.add(connectionRequest.getUserId());
			}
		}
	}

	private static void updateDocumentState(CourseState courseState, StreamDocumentAction action) {
		CourseStateDocument stateDocument = CourseStateDocument.builder()
				.documentId(action.getDocumentId())
				.type(action.getDocumentType().toString())
				.documentName(action.getDocumentTitle())
				.documentFile(action.getDocumentFile())
				.pages(new ConcurrentHashMap<>())
				.build();

		// System.out.println("doc: " + action.getDocumentId());

		courseState.addCourseStateDocument(stateDocument);
	}

	private static void updateActiveDocumentState(CourseState courseState, StreamDocumentAction action) {
		CourseStateDocument stateDocument = getCourseStateDocument(courseState, action.getDocumentId());

		// System.out.println("doc: " + action.getDocumentId());

		courseState.setActiveDocument(stateDocument);
	}

	private static void updateCreatedPageState(CourseState courseState, StreamPageAction action) {
		CourseStateDocument stateDocument = getCourseStateDocument(courseState, action.getDocumentId());
		CourseStatePage statePage = getCourseStatePage(stateDocument, action.getPageNumber(), true);

		// System.out.println("doc: " + action.getDocumentId() + ", page: " + action.getPageNumber());
	}

	private static void updateDeletedPageState(CourseState courseState, StreamPageAction action) {
		CourseStateDocument stateDocument = getCourseStateDocument(courseState, action.getDocumentId());

		deleteCourseStatePage(stateDocument, action.getPageNumber());

		// System.out.println("doc: " + action.getDocumentId() + ", page: " + action.getPageNumber());
	}

	private static void updateActivePageState(CourseState courseState, StreamPageAction action) {
		CourseStateDocument stateDocument = getCourseStateDocument(courseState, action.getDocumentId());
		CourseStatePage statePage = getCourseStatePage(stateDocument, action.getPageNumber(), true);

		// System.out.println("doc: " + action.getDocumentId() + ", page: " + action.getPageNumber());

		stateDocument.setActivePage(statePage);
	}

	private static void updatePageActionState(CourseState courseState, StreamPagePlaybackAction action) {
		CourseStateDocument stateDocument = getCourseStateDocument(courseState, action.getDocumentId());
		CourseStatePage statePage = getCourseStatePage(stateDocument, action.getPageNumber(), true);

		statePage.getActions().add(action.getAction());
	}

	private static void updatePageActionsState(CourseState courseState, StreamPageActionsAction action) {
		CourseStateDocument stateDocument = getCourseStateDocument(courseState, action.getDocumentId());
		CourseStatePage statePage = getCourseStatePage(stateDocument, action.getRecordedPage().getNumber(), true);

		// System.out.println("doc: " + action.getDocumentId() + ", page: " + action.getRecordedPage().getNumber());

		statePage.getActions().addAll(action.getRecordedPage().getPlaybackActions());
	}

	private static CourseStateDocument getCourseStateDocument(CourseState courseState, long documentId) {
		CourseStateDocument stateDocument = courseState.getCourseStateDocument(documentId);

		if (isNull(stateDocument)) {
			throw new IllegalStateException("Document does not exist");
		}

		return stateDocument;
	}

	private static CourseStatePage getCourseStatePage(CourseStateDocument stateDocument, int pageNumber, boolean createNew) {
		CourseStatePage statePage = stateDocument.getPages().get(pageNumber);

		if (isNull(statePage)) {
			if (createNew) {
				statePage = CourseStatePage.builder()
					.pageNumber(pageNumber)
					.actions(new ArrayList<>())
					.build();

				stateDocument.getPages().put(pageNumber, statePage);
			}
			else {
				throw new IllegalStateException("Page does not exist");
			}
		}

		return statePage;
	}

	private static void deleteCourseStatePage(CourseStateDocument stateDocument, int pageNumber) {
		CourseStatePage statePage = stateDocument.getPages().remove(pageNumber);

		if (isNull(statePage)) {
			
		}
	}

	private static StreamAction parseActionBuffer(ByteBuffer buffer) throws IOException {
		// Parse action header.
		int length = buffer.getInt();
		int type = buffer.get();

		byte[] actionData = null;
		int dataLength = length;

		if (dataLength > 0) {
			actionData = new byte[dataLength];
			buffer.get(actionData);
		}

		return StreamActionFactory.createAction(type, actionData);
	}
}
