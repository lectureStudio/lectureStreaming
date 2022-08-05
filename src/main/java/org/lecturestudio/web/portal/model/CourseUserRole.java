package org.lecturestudio.web.portal.model;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "course_users_roles")
@IdClass(CourseUserRoleId.class)
public class CourseUserRole {

	@Id
	String username;

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	Course course;

	@Id
	@ManyToOne
	Role role;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseUserRole)) {
			return false;
		}

		CourseUserRole that = (CourseUserRole) o;

		return course == that.course && Objects.equals(username, that.username) && Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username, course, role);
	}

	@Override
	public String toString() {
		return "CourseUserRole [course=" + course + ", role=" + role + ", username=" + username + "]";
	}
}
