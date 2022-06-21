package org.lecturestudio.web.portal.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.portal.model.User;
import org.springframework.stereotype.Service;

@Service    
public class CourseMessengerLogger {

    private final ConcurrentHashMap<Long, List<MessengerMessage>> courseMessengerLogs = new ConcurrentHashMap<>();

    public void addCourseLog(Long courseId) {
        List<MessengerMessage> courseLog = new ArrayList<MessengerMessage>();
        courseMessengerLogs.computeIfAbsent(courseId, new Function<Long, List<MessengerMessage>>() {

            @Override
            public List<MessengerMessage> apply(Long courseId) {
                return courseLog;
            }
        });
    }

    public void removeCourseLog(Long courseId) {
            courseMessengerLogs.remove(courseId);
    }


    public boolean logMessage(Long courseId, MessengerMessage message) {
        List<MessengerMessage> courseLog = courseMessengerLogs.get(courseId);

        if (Objects.nonNull(courseLog)) {
            Collections.synchronizedList(courseLog)
                .add(message);

            return true;
        }

        return false;
    }


    public List<MessengerMessage> getMessengerLogOfUser(Long courseId, User user) {
        List<MessengerMessage> courseMessengerLog = this.courseMessengerLogs.get(courseId);

        if (Objects.nonNull(courseMessengerLog)) {
            return courseMessengerLog.stream()
                .filter((message) -> {
                    return user.getUserId().equals(message.getRemoteAddress());
                })
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
