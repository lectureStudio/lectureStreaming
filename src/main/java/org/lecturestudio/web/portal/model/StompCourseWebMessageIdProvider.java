package org.lecturestudio.web.portal.model;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.lecturestudio.web.api.message.WebMessage;

public class StompCourseWebMessageIdProvider implements CourseWebMessageIdProvider {

    private final Long courseId;

    private final ConcurrentHashMap<String, AtomicLong> userMessageIdPostfix = new ConcurrentHashMap<>();


    public StompCourseWebMessageIdProvider(Long courseId) {
        this.courseId = courseId;
    }

    @Override
    public void setMessageId(WebMessage webMessage) {

        if (!Objects.isNull(webMessage.getUserId())) {
            StringBuilder sb = new StringBuilder();
            sb.append(courseId);
            sb.append("-");
            sb.append(webMessage.getUserId());
            sb.append("-");
            sb.append(this.getNextUserMessageIdPostfix(webMessage.getUserId()));

            webMessage.setMessageId(sb.toString());
        }
    }

    private Long getNextUserMessageIdPostfix(String username) {
        AtomicLong currUserPostId = userMessageIdPostfix.get(username);
        if (Objects.isNull(currUserPostId)) {
            currUserPostId = new AtomicLong();
            userMessageIdPostfix.put(username, currUserPostId);
        }
        return currUserPostId.getAndIncrement();
    }

}