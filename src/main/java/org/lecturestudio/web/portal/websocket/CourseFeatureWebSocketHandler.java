package org.lecturestudio.web.portal.websocket;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.api.message.WebMessage;
import org.lecturestudio.web.api.stream.action.StreamAction;
import org.lecturestudio.web.api.stream.action.StreamActionFactory;
import org.lecturestudio.web.api.stream.action.StreamActionType;
import org.lecturestudio.web.api.stream.action.StreamStartAction;
import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class CourseFeatureWebSocketHandler extends BinaryWebSocketHandler {

	private final Map<WebSocketSession, Long> sessions = new ConcurrentHashMap<>();

	private final Map<WebSocketSession, List<ByteBuffer>> sessionBufferMap = new ConcurrentHashMap<>();

	private final CourseMessengerFeatureSaveFeature messengerSaveFeature;

	private final CourseFeatureState courseFeatureState;

	private final ObjectMapper objectMapper;



	public CourseFeatureWebSocketHandler(CourseFeatureState featureState, ObjectMapper objectMapper, CourseMessengerFeatureSaveFeature messengerSaveFeature) {
		this.courseFeatureState = featureState;
		this.objectMapper = objectMapper;
		this.messengerSaveFeature = messengerSaveFeature;

		//courseFeatureState.addCourseFeatureListener(this::sendMessage);
		courseFeatureState.addCourseFeatureListener(this.messengerSaveFeature);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// No-op.
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// Clean-up state.
		sessionBufferMap.remove(session);
		sessions.remove(session);
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

	private void sendMessage(long courseId, WebMessage message) {
		WebSocketSession session = sessions.entrySet().stream()
			.filter(entry -> entry.getValue() == courseId)
			.findFirst()
			.map(Map.Entry::getKey)
			.orElse(null);

		if (nonNull(session)) {
			try {
				session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void sessionInit(WebSocketSession session, StreamStartAction initAction) {
		long courseId = initAction.getCourseId();

		// Bind session to the course ID.
		sessions.put(session, courseId);
	}

	private void parse(WebSocketSession session, ByteBuffer buffer) throws IOException {
		StreamAction action = parseActionBuffer(buffer);

		if (action.getType() == StreamActionType.STREAM_START) {
			sessionInit(session, (StreamStartAction) action);
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
