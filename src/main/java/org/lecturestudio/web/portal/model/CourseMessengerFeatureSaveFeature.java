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
import org.lecturestudio.web.api.message.WebMessage;

public class CourseMessengerFeatureSaveFeature implements CourseFeatureListener {

    private final Map<Long, List<WebMessage>> messengerMessageHistories = new ConcurrentHashMap<>();

    private final Map<Long, StompCourseWebMessageIdProvider> courseMessengerIdProviders = new ConcurrentHashMap<>();


	@Override
	public void onFeatureMessage(long courseId, WebMessage message) {
		if (message instanceof MessengerMessage || message instanceof MessengerDirectMessage) {
			this.onFeatureMessengerMessage(courseId, message);
		}
	}

	private void onFeatureMessengerMessage(long courseId, WebMessage message) {
		StompCourseWebMessageIdProvider courseMessengerIdProvider = courseMessengerIdProviders.get(courseId);

		if (isNull(courseMessengerIdProvider)) {
			courseMessengerIdProvider = new StompCourseWebMessageIdProvider(courseId);
			courseMessengerIdProviders.put(courseId, courseMessengerIdProvider);
		}
		courseMessengerIdProvider.setMessageId(message);

		List<WebMessage> messengerHistory = messengerMessageHistories.get(courseId);

		if (isNull(messengerHistory)) {
			List<WebMessage> futureHistory = Collections.synchronizedList(new LinkedList<>());
			futureHistory.add(message);
			messengerMessageHistories.put(courseId, futureHistory);
		}
		else {
			List<WebMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
			synchronizedMessengerHistory.add(message);
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

	public List<WebMessage> getMessengerHistoryOfCourse(long courseId, String userId) {
		List<WebMessage> historyList = messengerMessageHistories.get(courseId);

		if (nonNull(historyList)) {
			historyList = historyList.stream().filter(webMessage -> {
				if (webMessage instanceof MessengerDirectMessage) {
					MessengerDirectMessage directMessage = (MessengerDirectMessage) webMessage;
	
					// Return only private messages that we have sent or received.
					return directMessage.getUserId().equals(userId) || directMessage.getRecipient().equals(userId);
				}
	
				return true;
			}).collect(Collectors.toList());
		}

		return nonNull(historyList) ? historyList : List.of();
	}
}