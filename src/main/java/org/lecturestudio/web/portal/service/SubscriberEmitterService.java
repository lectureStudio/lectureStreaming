package org.lecturestudio.web.portal.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Service;

@Service
public class SubscriberEmitterService {

	private final SubscribableChannel subscribableChannel;

	private final Map<String, Set<MessageHandler>> messageHandlers;


	SubscriberEmitterService() {
		subscribableChannel = MessageChannels.publishSubscribe().get();
		messageHandlers = new ConcurrentHashMap<>();
	}

	public boolean send(Message<?> message) {
		return subscribableChannel.send(message);
	}

	public boolean send(String userId, Message<?> message) {
		Set<MessageHandler> handlers = messageHandlers.get(userId);

		if (isNull(handlers) || handlers.isEmpty()) {
			System.out.println("message could not be sent");
			return false;
		}

		for (MessageHandler handler : handlers) {
			handler.handleMessage(message);
		}

		return true;
	}

	public boolean subscribe(String userId, MessageHandler handler) {
		Set<MessageHandler> handlers = messageHandlers.get(userId);

		if (isNull(handlers)) {
			handlers = ConcurrentHashMap.newKeySet();

			messageHandlers.put(userId, handlers);
		}

		handlers.add(handler);

		return subscribableChannel.subscribe(handler);
	}

	public boolean unsubscribe(String userId, MessageHandler handler) {
		Set<MessageHandler> handlers = messageHandlers.get(userId);

		if (nonNull(handlers)) {
			handlers.remove(handler);

			if (handlers.isEmpty()) {
				messageHandlers.remove(userId);
			}
		}

		return subscribableChannel.unsubscribe(handler);
	}
}
