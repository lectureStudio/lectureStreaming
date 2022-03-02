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
public class CourseRoleId implements Serializable {

    private Long courseId;

    private Long roleId;

    public static CourseRoleId getIdFrom(Course course, Role role) {
        return new CourseRoleId(course.getId(), role.getId());
    }
    
}
