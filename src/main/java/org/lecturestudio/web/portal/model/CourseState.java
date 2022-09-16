package org.lecturestudio.web.portal.model;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lecturestudio.web.portal.service.UserService;

public class CourseState {

	private final UserService userService;

	private final long courseId;

	private final long timestamp;

	private final Map<String, Set<BigInteger>> participantMap = new ConcurrentHashMap<>();

	private final Map<Long, CourseStateDocument> documentMap = new ConcurrentHashMap<>();

	private CourseStateDocument avtiveDocument;

	private boolean isRecorded;


	public CourseState(UserService userService, long courseId) {
		this.userService = userService;
		this.courseId = courseId;
		timestamp = System.currentTimeMillis();
	}

	public long getCreatedTimestamp() {
		return timestamp;
	}

	public boolean getRecordedState() {
		return isRecorded;
	}

	public void setRecordedState(boolean recorded) {
		this.isRecorded = recorded;
	}

	/**
	 * Set only if the corresponding participant has been registered.
	 * 
	 * @param participantId The unique participant ID.
	 * @param sessionId     The unique streaming session ID.
	 */
	public synchronized void setParticipantSession(String participantId, BigInteger sessionId) {
		if (isNull(participantId)) {
			return;
		}

		Set<BigInteger> openSessions = participantMap.get(participantId);
		if (isNull(openSessions)) {
			openSessions = new HashSet<>();
			participantMap.put(participantId, openSessions);
		}

		if (openSessions.isEmpty()) {
			// Optional<User> user = userService.findById(participantId);

			// Send notification of arrival.
			// CourseParticipantMessage participantMessage = new CourseParticipantMessage();
			// participantMessage.setConnected(true);

			// if (user.isPresent()) {
			// 	participantMessage.setFirstName(user.get().getFirstName());
			// 	participantMessage.setFamilyName(user.get().getFamilyName());
			// 	participantMessage.setUserName(user.get().getUserId());
			// }

			// postParticipantMessage(courseId, participantMessage);
		}

		openSessions.add(sessionId);
	}

	/**
	 * Unregisters a participant with the given session ID.
	 * 
	 * @param sessionId The unique streaming session ID.
	 */
	public synchronized void removeParticipantWithSessionId(BigInteger sessionId) {
		for (var entry : participantMap.entrySet()) {
			if (entry.getValue().contains(sessionId)) {
				String participantId = entry.getKey();

				entry.getValue().remove(sessionId);

				if (entry.getValue().isEmpty()) {
					// Optional<User> user = userService.findById(participantId);

					// // Send notification of departure.
					// CourseParticipantMessage participantMessage = new CourseParticipantMessage();
					// participantMessage.setConnected(false);

					// if (user.isPresent()) {
					// 	participantMessage.setFirstName(user.get().getFirstName());
					// 	participantMessage.setFamilyName(user.get().getFamilyName());
					// 	participantMessage.setUserName(user.get().getUserId());
					// }

					// postParticipantMessage(courseId, participantMessage);
				}

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
}
