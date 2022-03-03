package org.lecturestudio.web.portal.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseUserId implements Serializable {

    private Long courseId;

    private String userId;

    public static CourseUserId getIdFrom(Course course, User user) {
        return new CourseUserId(course.getId(), user.getUserId());
    }
    
}
