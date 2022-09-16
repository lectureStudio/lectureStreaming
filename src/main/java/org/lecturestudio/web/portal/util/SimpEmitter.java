package org.lecturestudio.web.portal.util;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SimpEmitter {

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Value("${simp.destinations.event}")
	private String eventDestination;


	public void emmitChatMessage(Long courseId, Object payload) {
		simpMessagingTemplate.convertAndSend("/topic/course/" + courseId + "/chat", payload,
				Map.of("payloadType", payload.getClass().getSimpleName()));
	}

	public void emmitChatMessageToUser(Long courseId, Object payload, String user) {
		simpMessagingTemplate.convertAndSendToUser(user, "/queue/course/" + courseId + "/chat",
				payload, Map.of("payloadType", payload.getClass().getSimpleName()));
	}

	public void emmitEvent(Long courseId, String eventName, Object payload) {
		simpMessagingTemplate.convertAndSend(eventDestination + courseId + "/" + eventName, payload,
				Map.of("payloadType", payload.getClass().getSimpleName()));
	}

	public void emmitEventAndAll(Long courseId, String eventName, Object payload) {
		emmitEvent(courseId, eventName, payload);

		simpMessagingTemplate.convertAndSend(eventDestination + "all/" + eventName, payload,
				Map.of("payloadType", payload.getClass().getSimpleName()));
	}
}
