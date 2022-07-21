package org.lecturestudio.web.portal.model;

import java.io.Serializable;
import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseUserId)) {
			return false;
		}

		CourseUserId that = (CourseUserId) o;

		return Objects.equals(courseId, that.courseId) && Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(courseId, userId);
	}
}