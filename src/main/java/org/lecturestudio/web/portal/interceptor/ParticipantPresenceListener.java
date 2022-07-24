package org.lecturestudio.web.portal.interceptor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.lecturestudio.web.api.message.CoursePresenceMessage;
import org.lecturestudio.web.api.stream.model.CoursePresence;
import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.CourseParticipantService;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class ParticipantPresenceListener {

	@Autowired
	private UserService userService;

	@Autowired
	private CourseParticipantService participantService;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;


	@EventListener
	private void handleSessionConnected(SessionConnectEvent event) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionHeaders = headers.getSessionAttributes();
		String stompEndpoint = (String) sessionHeaders.get("stomp-endpoint");
		String userName = headers.getUser().getName();

		if ("/ws-state".equals(stompEndpoint)) {
			User user = userService.findById(userName).orElse(null);
			String courseId = headers.getFirstNativeHeader("courseId");

			if (nonNull(user) && isNumeric(courseId)) {
				CoursePresenceMessage presenceMessage = new CoursePresenceMessage();
				presenceMessage.setDate(ZonedDateTime.now());
				presenceMessage.setFamilyName(user.getFamilyName());
				presenceMessage.setFirstName(user.getFirstName());
				presenceMessage.setUserId(userName);
				presenceMessage.setCoursePresence(CoursePresence.CONNECTED);

				simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/presence", presenceMessage,
						Map.of("payloadType", presenceMessage.getClass().getSimpleName()));

				CourseParticipant participant = CourseParticipant.builder()
					.courseId(Long.parseLong(courseId))
					.userId(userName)
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
		String stompEndpoint = (String) sessionHeaders.get("stomp-endpoint");
		String userName = headers.getUser().getName();

		if ("/ws-state".equals(stompEndpoint)) {
			User user = userService.findById(userName).orElse(null);
			CourseParticipant participant = participantService.getParticipantBySessionId(headers.getSessionId()).orElse(null);

			if (nonNull(participant)) {
				Long courseId = participant.getCourseId();

				CoursePresenceMessage presenceMessage = new CoursePresenceMessage();
				presenceMessage.setDate(ZonedDateTime.now());
				presenceMessage.setFamilyName(user.getFamilyName());
				presenceMessage.setFirstName(user.getFirstName());
				presenceMessage.setUserId(userName);
				presenceMessage.setCoursePresence(CoursePresence.DISCONNECTED);

				simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/presence", presenceMessage,
					Map.of("payloadType", presenceMessage.getClass().getSimpleName()));
			}

			participantService.deleteParticipantBySessionId(headers.getSessionId());
		}
		

		// Optional.ofNullable(participantRepository.getParticipant(event.getSessionId()))
		// 		.ifPresent(login -> {
		// 			messagingTemplate.convertAndSend(logoutDestination, new LogoutEvent(login.getUsername()));
		// 			participantRepository.removeParticipant(event.getSessionId());
		// 		});
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
