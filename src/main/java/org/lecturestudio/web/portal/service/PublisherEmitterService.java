package org.lecturestudio.web.portal.service;

import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Service;

@Service
public class PublisherEmitterService {

	private final SubscribableChannel subscribableChannel;


	PublisherEmitterService() {
		subscribableChannel = MessageChannels.publishSubscribe().get();
	}

	public boolean send(Message<?> message) {
		return subscribableChannel.send(message);
	}

	public boolean subscribe(MessageHandler handler) {
		return subscribableChannel.subscribe(handler);
	}

	public boolean unsubscribe(MessageHandler handler) {
		return subscribableChannel.unsubscribe(handler);
	}
}
