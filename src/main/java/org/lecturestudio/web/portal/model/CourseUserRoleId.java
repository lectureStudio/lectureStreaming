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
public class CourseUserRoleId implements Serializable {

	private long course;

	private long role;

	private String username;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseUserRoleId)) {
			return false;
		}

		CourseUserRoleId that = (CourseUserRoleId) o;

		return Objects.equals(course, that.course)
			&& Objects.equals(role, that.role)
			&& Objects.equals(username, that.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(course, role, username);
	}

}
