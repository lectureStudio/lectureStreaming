package org.lecturestudio.web.portal.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.lecturestudio.web.api.message.CourseParticipantMessage;
import org.lecturestudio.web.api.message.SpeechBaseMessage;
import org.lecturestudio.web.portal.service.UserService;

public class CourseState {

	private final UserService userService;

	private final long courseId;

	private final BiConsumer<Long, SpeechBaseMessage> speechListener;

	private final BiConsumer<Long, CourseParticipantMessage> connectedListener;

	private final long timestamp;

	private final Map<String, BigInteger> participantMap = new ConcurrentHashMap<>();

	private final Map<Long, CourseStateDocument> documentMap = new ConcurrentHashMap<>();

	private CourseStateDocument avtiveDocument;


	public CourseState(UserService userService, long courseId, BiConsumer<Long, SpeechBaseMessage> speechListener, BiConsumer<Long, CourseParticipantMessage> connectedListener) {
		this.userService = userService;
		this.courseId = courseId;
		this.speechListener = speechListener;
		this.connectedListener = connectedListener;
		timestamp = System.currentTimeMillis();
	}

	public void postSpeechMessage(Long courseId, SpeechBaseMessage message) {
		speechListener.accept(courseId, message);
	}

	public long getCreatedTimestamp() {
		return timestamp;
	}

	/**
	 * Set only if the corresponding participant has been registered.
	 * 
	 * @param participantId The unique participant ID.
	 * @param sessionId     The unique streaming session ID.
	 */
	public void setParticipantSession(String participantId, BigInteger sessionId) {
		if (isNull(participantId)) {
			return;
		}

		if (nonNull(participantMap.get(participantId))) {
			// Already joined.
			return;
		}

		participantMap.put(participantId, sessionId);

		Optional<User> user = userService.findById(participantId);

		if (!user.isPresent()) {
			// TODO: kick participant from course?
			return;
		}

		// Send notification of arrival.
		CourseParticipantMessage participantMessage = new CourseParticipantMessage();
		participantMessage.setConnected(true);
		participantMessage.setFirstName(user.get().getFirstName());
		participantMessage.setFamilyName(user.get().getFamilyName());

		postParticipantMessage(courseId, participantMessage);
	}

	/**
	 * Unregisters a participant with the given session ID.
	 * 
	 * @param sessionId The unique streaming session ID.
	 */
	public void removeParticipantWithSessionId(BigInteger sessionId) {
		for (var entry : participantMap.entrySet()) {
			if (entry.getValue().equals(sessionId)) {
				String participantId = entry.getKey();

				participantMap.remove(participantId);

				Optional<User> user = userService.findById(participantId);

				// Send notification of departure.
				CourseParticipantMessage participantMessage = new CourseParticipantMessage();
				participantMessage.setConnected(false);

				if (user.isPresent()) {
					participantMessage.setFirstName(user.get().getFirstName());
					participantMessage.setFamilyName(user.get().getFamilyName());
				}

				postParticipantMessage(courseId, participantMessage);
				break;
			}
		}
	}

	public CourseStateDocument getActiveDocument() {
		return avtiveDocument;
	}

	public void setActiveDocument(CourseStateDocument document) {
		this.avtiveDocument = document;
	}

	public Map<Long, CourseStateDocument> getAllCourseStateDocuments() {
		return documentMap;
	}

	public CourseStateDocument getCourseStateDocument(Long docId) {
		return documentMap.get(docId);
	}

	public void addCourseStateDocument(CourseStateDocument stateDoc) {
		documentMap.put(stateDoc.getDocumentId(), stateDoc);
	}

	public void removeCourseStateDocument(CourseStateDocument stateDoc) {
		documentMap.remove(stateDoc.getDocumentId());
	}

	private void postParticipantMessage(Long courseId, CourseParticipantMessage message) {
		connectedListener.accept(courseId, message);
	}
}
