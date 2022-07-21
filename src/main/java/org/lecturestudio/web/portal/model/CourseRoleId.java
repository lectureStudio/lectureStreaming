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
public class CourseRoleId implements Serializable {

	private Long courseId;

	private Long roleId;


	public static CourseRoleId getIdFrom(Course course, Role role) {
		return new CourseRoleId(course.getId(), role.getId());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseRoleId)) {
			return false;
		}

		CourseRoleId that = (CourseRoleId) o;

		return Objects.equals(courseId, that.courseId) && Objects.equals(roleId, that.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(courseId, roleId);
	}
}