package org.lecturestudio.web.portal.model;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.lecturestudio.web.api.message.CourseParticipantMessage;
import org.lecturestudio.web.api.message.SpeechBaseMessage;

public class CourseState {

	private final long courseId;

	private final BiConsumer<Long, SpeechBaseMessage> speechListener;

	private final BiConsumer<Long, CourseParticipantMessage> connectedListener;

	private final long timestamp;

	private final Set<BigInteger> sessions = ConcurrentHashMap.newKeySet();

	private final Map<Long, CourseStateDocument> documentMap = new ConcurrentHashMap<>();

	private CourseStateDocument avtiveDocument;

	public CourseState(long courseId, BiConsumer<Long, SpeechBaseMessage> speechListener, BiConsumer<Long, CourseParticipantMessage> connectedListener) {
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

	public void addSessionId(BigInteger sessionId) {
		if (sessions.add(sessionId)) {
			CourseParticipantMessage participantMessage = new CourseParticipantMessage();
			participantMessage.setConnected(true);

			postParticipantMessage(courseId, participantMessage);
		}
	}

	public void removeSessionId(BigInteger sessionId) {
		if (sessions.remove(sessionId)) {
			CourseParticipantMessage participantMessage = new CourseParticipantMessage();
			participantMessage.setConnected(false);

			postParticipantMessage(courseId, participantMessage);
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

	public void postParticipantMessage(Long courseId, CourseParticipantMessage message) {
		connectedListener.accept(courseId, message);
	}
}
