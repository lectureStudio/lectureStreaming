package org.lecturestudio.web.portal.interceptor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Map;

import javax.transaction.Transactional;

import org.lecturestudio.web.api.stream.model.CoursePresence;
import org.lecturestudio.web.api.stream.model.CoursePresenceType;
import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.CourseParticipantService;
import org.lecturestudio.web.portal.service.CoursePresenceService;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class ParticipantPresenceListener {

	@Autowired
	private CoursePresenceService coursePresenceService;

	@Autowired
	private UserService userService;

	@Autowired
	private CourseParticipantService participantService;

	@Value("${simp.endpoints.publisher}")
	private String publisherEndpoint;

	@Value("${simp.endpoints.state}")
	private String stateEndpoint;

	@Value("${simp.session.header.endpoint}")
	private String endpointHeader;


	@EventListener
	private void handleSessionConnected(SessionConnectEvent event) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionHeaders = headers.getSessionAttributes();
		String stompEndpoint = (String) sessionHeaders.get(endpointHeader);
		String userName = headers.getUser().getName();

		if (stateEndpoint.equals(stompEndpoint) || publisherEndpoint.equals(stompEndpoint)) {
			User user = userService.findById(userName).orElse(null);
			String courseIdStr = headers.getFirstNativeHeader("courseId");

			if (nonNull(user) && isNumeric(courseIdStr)) {
				Long courseId = Long.parseLong(courseIdStr);

				if (!participantService.existsByUserId(userName)) {
					coursePresenceService.sendCoursePresence(CoursePresence.CONNECTED, CoursePresenceType.CLASSROOM, user, courseId);
				}

				CourseParticipant participant = CourseParticipant.builder()
						.courseId(courseId)
						.user(user)
						.sessionId(headers.getSessionId())
						.build();

				// Store the session as we need it to be idempotent in the disconnect event processing.
				participantService.saveParticipant(participant);
			}
		}
	}

	@EventListener
	@Transactional
	private void handleSessionDisconnect(SessionDisconnectEvent event) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionHeaders = headers.getSessionAttributes();
		String stompEndpoint = (String) sessionHeaders.get(endpointHeader);
		String userName = headers.getUser().getName();

		if (stateEndpoint.equals(stompEndpoint) || publisherEndpoint.equals(stompEndpoint)) {
			CourseParticipant participant = participantService.getParticipantBySessionId(headers.getSessionId()).orElse(null);

			if (nonNull(participant) && participantService.existsByUserId(userName)) {
				Long courseId = participant.getCourseId();

				coursePresenceService.sendCoursePresence(CoursePresence.DISCONNECTED, CoursePresenceType.CLASSROOM, userName, courseId);
			}

			participantService.deleteParticipantBySessionId(headers.getSessionId());
		}
	}

	public static boolean isNumeric(String strNum) {
		if (isNull(strNum)) {
			return false;
		}

		try {
			Long.parseLong(strNum);
		}
		catch (NumberFormatException nfe) {
			return false;
		}

		return true;
	}
}
