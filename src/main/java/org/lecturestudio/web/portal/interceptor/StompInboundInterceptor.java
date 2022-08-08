package org.lecturestudio.web.portal.interceptor;

import static java.util.Objects.isNull;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.service.CourseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

public class StompInboundInterceptor implements ChannelInterceptor {

	private static final Pattern COURSE_TOPIC = Pattern.compile("/topic/course/event/(\\d+)/(\\w+)");

	private static final Pattern COURSE_CHAT = Pattern.compile("/topic/course/(\\d+)/chat");

	private static final Pattern COURSE_USER_CHAT = Pattern.compile("/user/queue/course/(\\d+)/chat");

	@Autowired
	private CourseService courseService;

	@Value("${simp.events.chat}")
	private String chatEvent;

	@Value("${simp.events.presence}")
	private String presenceEvent;

	@Value("${simp.events.speech}")
	private String speechEvent;

	@Value("${simp.events.quiz}")
	private String quizEvent;

	@Value("${simp.session.header.endpoint}")
	private String endpointHeader;

	@Value("${simp.endpoints.state}")
	private String stateEndpoint;


	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompCommand messageCommand = accessor.getCommand();

		Map<String, Object> sessionHeaders = accessor.getSessionAttributes();
		String stompEndpoint = (String) sessionHeaders.get(endpointHeader);

		if (StompCommand.SUBSCRIBE.equals(messageCommand)) {
			if (stateEndpoint.equals(stompEndpoint)) {
				Principal principal = accessor.getUser();
				String destination = accessor.getDestination();

				try {
					checkTopicPermission(destination, principal);
					checkChatPermission(destination, principal);
					checkUserChatPermission(destination, principal);
				}
				catch (UnauthorizedException e) {
					System.out.println(accessor.getUser().getName() + " not authorized for " + destination);
					return null;
				}
			}
		}

		return message;
	}

	private void checkTopicPermission(String destination, Principal principal) {
		Matcher matcher = COURSE_TOPIC.matcher(destination);

		if (matcher.find()) {
			String courseIdStr = matcher.group(1);
			String topic = matcher.group(2);

			if (!isNumeric(courseIdStr) || isNull(topic)) {
				throw new UnauthorizedException();
			}

			long courseId = Long.parseLong(courseIdStr);
			boolean authorized = true;

			if (topic.equals(chatEvent)) {
				authorized = courseService.isAuthorized(courseId, principal, "CHAT_READ");
			}
			else if (topic.equals(presenceEvent)) {
				authorized = courseService.isAuthorized(courseId, principal, "PARTICIPANTS_VIEW");
			}
			else if (topic.equals(speechEvent)) {
				authorized = courseService.isAuthorized(courseId, principal, "SPEECH");
			}
			else if (topic.equals(quizEvent)) {
				authorized = courseService.isAuthorized(courseId, principal, "QUIZ_PARTICIPATION");
			}

			if (!authorized) {
				throw new UnauthorizedException();
			}
		}
	}

	private void checkChatPermission(String destination, Principal principal) {
		Matcher matcher = COURSE_CHAT.matcher(destination);

		if (matcher.find()) {
			String courseIdStr = matcher.group(1);

			if (!isNumeric(courseIdStr)) {
				throw new UnauthorizedException();
			}

			long courseId = Long.parseLong(courseIdStr);

			if (!courseService.isAuthorized(courseId, principal, "CHAT_READ")) {
				throw new UnauthorizedException();
			}
		}
	}

	private void checkUserChatPermission(String destination, Principal principal) {
		Matcher matcher = COURSE_USER_CHAT.matcher(destination);

		if (matcher.find()) {
			String courseIdStr = matcher.group(1);

			if (!isNumeric(courseIdStr)) {
				throw new UnauthorizedException();
			}

			long courseId = Long.parseLong(courseIdStr);

			if (!courseService.isAuthorized(courseId, principal, "CHAT_READ")) {
				throw new UnauthorizedException();
			}
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
