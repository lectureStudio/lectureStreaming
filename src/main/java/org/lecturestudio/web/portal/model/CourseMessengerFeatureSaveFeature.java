package org.lecturestudio.web.portal.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.WebMessage;

public class CourseMessengerFeatureSaveFeature implements CourseFeatureListener {

    private final ConcurrentHashMap<Long, List<MessengerMessage>> messengerMessageHistories = new ConcurrentHashMap<>();


    @Override
    public void onFeatureMessage(long courseId, WebMessage message) {
        if (message instanceof MessengerMessage) {
            MessengerMessage mMessage = (MessengerMessage) message;

            List<MessengerMessage> messengerHistory = messengerMessageHistories.get(courseId);

            if (Objects.isNull(messengerHistory)) {
                List<MessengerMessage> futureHistory = Collections.synchronizedList(new LinkedList<MessengerMessage>());
                futureHistory.add(mMessage);
                messengerMessageHistories.put(courseId, futureHistory);
            }
            else {
                List<MessengerMessage> synchronizedMessengerHistory = Collections.synchronizedList(messengerHistory);
                synchronizedMessengerHistory.add(mMessage);
            }
        }
    }

    public void addCourseHistory(long courseId) {
        List<MessengerMessage> futureHistory = new LinkedList<MessengerMessage>();
        messengerMessageHistories.put(courseId, futureHistory);
    }

    public void removeCourseHistory(long courseId) {
        messengerMessageHistories.remove(courseId);
    }

    public List<MessengerMessage> getMessengerHistoryOfCourse(long courseId) {
        List<MessengerMessage> messengerHistoryOfCourse = messengerMessageHistories.get(courseId);
        
        if (!Objects.isNull(messengerHistoryOfCourse)) {
            List<MessengerMessage> messengerHistoryOfCourseSynchronized = Collections.synchronizedList(messengerHistoryOfCourse);
            synchronized(messengerHistoryOfCourseSynchronized ) {
                return new LinkedList<MessengerMessage>(messengerHistoryOfCourseSynchronized);
            }
        }

        return new LinkedList<MessengerMessage>();
    }
    
}
