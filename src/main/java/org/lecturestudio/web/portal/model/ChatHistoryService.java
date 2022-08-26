package org.lecturestudio.web.portal.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.lecturestudio.web.api.message.MessengerDirectMessage;
import org.lecturestudio.web.api.message.MessengerMessage;

public class ChatHistoryService {

    private final Map<Long, List<MessengerMessage>> messengerMessageHistories = new ConcurrentHashMap<>();

    private final Map<Long, StompCourseWebMessageIdProvider> courseMessengerIdProviders = new ConcurrentHashMap<>();


	public void addMessage(long courseId, MessengerMessage message) {
		StompCourseWebMessageIdProvider courseMessengerIdProvider = courseMessengerIdProviders.get(courseId);

		if (isNull(courseMessengerIdProvider)) {
			courseMessengerIdProvider = new StompCourseWebMessageIdProvider(courseId);
			courseMessengerIdProviders.put(courseId, courseMessengerIdProvider);
		}
		courseMessengerIdProvider.setMessageId(message);

		List<MessengerMessage> messengerHistory = messengerMessageHistories.get(courseId);

		if (isNull(messengerHistory)) {
			List<MessengerMessage> futureHistory = Collections.synchronizedList(new LinkedList<>());
			futureHistory.add(message);
			messengerMessageHistories.put(courseId, futureHistory);
		}
		else {
			List<MessengerMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
			synchronizedMessengerHistory.add(message);
		}
	}

	public void addMessage(long courseId, MessengerMessage message, String orgaRecipient) {
		if (message instanceof MessengerDirectMessage) {
			MessengerDirectMessage directMessage = (MessengerDirectMessage) message;

			addMessage(courseId, new DirectOrgaMessage(directMessage, orgaRecipient));
		}
		else {
			addMessage(courseId, message);
		}
	}

	public void createCourseHistory(long courseId) {
		List<MessengerMessage> futureHistory = new LinkedList<>();
		messengerMessageHistories.put(courseId, futureHistory);
		courseMessengerIdProviders.put(courseId, new StompCourseWebMessageIdProvider(courseId));
	}

	public void removeCourseHistory(long courseId) {
		messengerMessageHistories.remove(courseId);
		courseMessengerIdProviders.remove(courseId);
	}

	public List<MessengerMessage> getCourseHistory(long courseId, String userId) {
		List<MessengerMessage> historyList = messengerMessageHistories.get(courseId);

		if (nonNull(historyList)) {
			historyList = historyList.stream().filter(message -> {
				if (message instanceof DirectOrgaMessage) {
					DirectOrgaMessage directMessage = (DirectOrgaMessage) message;

					// Return only private messages that we have sent or received.
					return directMessage.orgaRecipient.equals(userId);
				}
				if (message instanceof MessengerDirectMessage) {
					MessengerDirectMessage directMessage = (MessengerDirectMessage) message;

					// Return only private messages that we have sent or received.
					return directMessage.getUserId().equals(userId) || directMessage.getRecipientId().equals(userId);
				}

				return true;
			}).collect(Collectors.toList());
		}

		return nonNull(historyList) ? historyList : List.of();
	}



	private static class DirectOrgaMessage extends MessengerDirectMessage {

		String orgaRecipient;


		DirectOrgaMessage(MessengerDirectMessage message, String orgaRecipient) {
			super(message);

			this.orgaRecipient = orgaRecipient;
		}

	}
}