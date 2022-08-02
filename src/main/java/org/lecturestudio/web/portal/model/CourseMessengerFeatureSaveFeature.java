package org.lecturestudio.web.portal.model;

import static java.util.Objects.nonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.lecturestudio.web.api.message.MessengerDirectMessage;
import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.MessengerReplyMessage;
import org.lecturestudio.web.api.message.WebMessage;

public class CourseMessengerFeatureSaveFeature implements CourseFeatureListener {

    private final Map<Long, List<WebMessage>> messengerMessageHistories = new ConcurrentHashMap<>();

    private final Map<Long, StompCourseWebMessageIdProvider> courseMessengerIdProviders = new ConcurrentHashMap<>();


	@Override
	public void onFeatureMessage(long courseId, WebMessage message) {
		if (message instanceof MessengerMessage || message instanceof MessengerDirectMessage) {
			this.onFeatureMessengerMessage(courseId, message);
		}
		else if (message instanceof MessengerReplyMessage) {
			MessengerReplyMessage mReplyMessage = (MessengerReplyMessage) message;
			this.onFeaturMessengerReplyMessage(courseId, mReplyMessage);
		}
	}

	private void onFeatureMessengerMessage(long courseId, WebMessage message) {
		StompCourseWebMessageIdProvider courseMessengerIdProvider = courseMessengerIdProviders.get(courseId);

		if (Objects.isNull(courseMessengerIdProvider)) {
			courseMessengerIdProvider = new StompCourseWebMessageIdProvider(courseId);
			courseMessengerIdProviders.put(courseId, courseMessengerIdProvider);
		}
		courseMessengerIdProvider.setMessageId(message);

		List<WebMessage> messengerHistory = messengerMessageHistories.get(courseId);

		if (Objects.isNull(messengerHistory)) {
			List<WebMessage> futureHistory = Collections.synchronizedList(new LinkedList<WebMessage>());
			futureHistory.add(message);
			messengerMessageHistories.put(courseId, futureHistory);
		}
		else {
			List<WebMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
			synchronizedMessengerHistory.add(message);
		}
	}

	private void onFeaturMessengerReplyMessage(long courseId, MessengerReplyMessage mReplyMessage) {
		List<WebMessage> messengerHistory = messengerMessageHistories.get(courseId);

		if (!Objects.isNull(messengerHistory)) {
			List<WebMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
			List<WebMessage> repliedMessages = synchronizedMessengerHistory.stream().filter((message) -> {
				if (message instanceof MessengerMessage || message instanceof MessengerDirectMessage) {
					return message.getMessageId().equals(mReplyMessage.getRepliedMessageId());
				}
				return false;
			}).toList();

			repliedMessages.forEach((message) -> {
				if (message instanceof MessengerMessage) {
					((MessengerMessage) message).setReply(true);
				} else if (message instanceof MessengerDirectMessage) {
					((MessengerDirectMessage) message).setReply(true);
				}
			});
		}
	}

	public void addCourseHistory(long courseId) {
		List<WebMessage> futureHistory = new LinkedList<WebMessage>();
		messengerMessageHistories.put(courseId, futureHistory);
		courseMessengerIdProviders.put(courseId, new StompCourseWebMessageIdProvider(courseId));
	}

	public void removeCourseHistory(long courseId) {
		messengerMessageHistories.remove(courseId);
		courseMessengerIdProviders.remove(courseId);
	}

	public List<WebMessage> getMessengerHistoryOfCourse(long courseId, User user) {
		List<WebMessage> historyList = messengerMessageHistories.get(courseId);

		return nonNull(historyList) ? historyList : List.of();
	}
}