package org.lecturestudio.web.portal.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.MessengerReplyMessage;
import org.lecturestudio.web.api.message.SpeechBaseMessage;
import org.lecturestudio.web.api.message.WebMessage;

public class CourseMessengerFeatureSaveFeature implements CourseFeatureListener {

    private final ConcurrentHashMap<Long, List<WebMessage>> messengerMessageHistories = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, StompCourseWebMessageIdProvider> courseMessengerIdProviders = new ConcurrentHashMap<>();


    @Override
    public void onFeatureMessage(long courseId, WebMessage message) {
        if (message instanceof MessengerMessage) {
            MessengerMessage mMessage = (MessengerMessage) message;
            this.onFeatureMessengerMessage(courseId, mMessage);
        }
        else if (message instanceof MessengerReplyMessage) {
            MessengerReplyMessage mReplyMessage = (MessengerReplyMessage) message;
            this.onFeaturMessengerReplyMessage(courseId, mReplyMessage);
        }
    }

    private void onFeatureMessengerMessage(long courseId, MessengerMessage mMessage) {
        StompCourseWebMessageIdProvider courseMessengerIdProvider = courseMessengerIdProviders.get(courseId);

        if (Objects.isNull(courseMessengerIdProvider)) {
            courseMessengerIdProvider = new StompCourseWebMessageIdProvider(courseId);
            courseMessengerIdProviders.put(courseId, courseMessengerIdProvider);
        }
        courseMessengerIdProvider.setMessageId(mMessage);
    

        List<WebMessage> messengerHistory = messengerMessageHistories.get(courseId);

        if (Objects.isNull(messengerHistory)) {
            List<WebMessage> futureHistory = Collections.synchronizedList(new LinkedList<WebMessage>());
            futureHistory.add(mMessage);
            messengerMessageHistories.put(courseId, futureHistory);
        }
        else {
            List<WebMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
            synchronizedMessengerHistory.add(mMessage);
        }
    }

    private void onFeaturMessengerReplyMessage(long courseId, MessengerReplyMessage mReplyMessage) {
        List<WebMessage> messengerHistory = messengerMessageHistories.get(courseId);

        if (! Objects.isNull(messengerHistory)) {
            List<WebMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
            List<WebMessage> repliedMessages = synchronizedMessengerHistory.stream().filter((message) -> {
                if (message instanceof MessengerMessage) {
                    return message.getMessageId().equals(mReplyMessage.getRepliedMessageId());
                }
                return false;
            }).toList();

            repliedMessages.forEach((message) -> {
                ((MessengerMessage) message).setReply(true);
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

    public List<WebMessage> getMessengerHistoryOfCourseBidirectional(long courseId, User user) {
        List<WebMessage> messengerHistoryOfCourse = messengerMessageHistories.get(courseId);
        
        if (!Objects.isNull(messengerHistoryOfCourse)) {
            List<WebMessage> messengerHistoryOfCourseSynchronized = Collections.synchronizedList(messengerHistoryOfCourse);
            synchronized(messengerHistoryOfCourseSynchronized ) {
                List<WebMessage> messengerHistoryOfCourseFiltered = messengerHistoryOfCourseSynchronized.stream().filter((message) -> {
                    if (message instanceof MessengerMessage) {
                        return true;
                    }
                    else if (message instanceof SpeechBaseMessage) {
                        return message.getRemoteAddress().equals(user.getUserId());
                    }
                    else {
                        return false;
                    }
                }).collect(Collectors.toList());
                return new LinkedList<WebMessage>(messengerHistoryOfCourseFiltered);
            }
        }

        return new LinkedList<WebMessage>();
    }

    public List<WebMessage> getMessengerHistoryOfCourseUnidirectional(long courseId, User user) {
        List<WebMessage> messengerHistoryOfCourse = messengerMessageHistories.get(courseId);
        
        if (!Objects.isNull(messengerHistoryOfCourse)) {
            List<WebMessage> messengerHistoryOfCourseSynchronized = Collections.synchronizedList(messengerHistoryOfCourse);
            synchronized(messengerHistoryOfCourseSynchronized ) {
                List<WebMessage> messengerHistoryOfCourseFiltered = messengerHistoryOfCourseSynchronized.stream().filter((message) -> {
                    if (message instanceof MessengerMessage || message instanceof SpeechBaseMessage) {
                        return message.getRemoteAddress().equals(user.getUserId());
                    }
                    else {
                        return false;
                    }
                }).collect(Collectors.toList());
                return new LinkedList<WebMessage>(messengerHistoryOfCourseFiltered);
            }
        }

        return new LinkedList<WebMessage>();
    }
    
}
