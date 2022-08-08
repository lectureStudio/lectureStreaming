package org.lecturestudio.web.portal.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@IdClass(CourseUserRoleId.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseUserRole {

	@Id
	@Column(name = "user_id")
	String userId;

	@Id
	@ManyToOne
	@JoinColumn(name = "role_id")
	Role role;

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id")
	Course course;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseUserRole)) {
			return false;
		}

		CourseUserRole that = (CourseUserRole) o;

		return course == that.course && Objects.equals(userId, that.userId) && Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, course, role);
	}

	@Override
	public String toString() {
		return "CourseUserRole [course=" + course + ", role=" + role + ", userId=" + userId + "]";
	}
}
