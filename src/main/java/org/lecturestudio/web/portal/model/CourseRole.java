package org.lecturestudio.web.portal.model;

import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
@Table(name = "course_roles")
public class CourseRole {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	Long id;

	@ManyToOne
	@JoinColumn(name = "role_id")
	Role role;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.MERGE, CascadeType.PERSIST })
	@JoinColumn(name = "course_id")
	Course course;

	@ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REFRESH, CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "course_roles_privileges",
		joinColumns = @JoinColumn(name = "course_role_id", referencedColumnName = "id"),
		inverseJoinColumns = @JoinColumn(name = "course_privilege_id", referencedColumnName = "id"))
	Set<CoursePrivilege> privileges;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseRole)) {
			return false;
		}

		CourseRole that = (CourseRole) o;

		return Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(role);
	}

	@Override
	public String toString() {
		return "CourseRole [name=" + role.getName() + "]";
	}
}