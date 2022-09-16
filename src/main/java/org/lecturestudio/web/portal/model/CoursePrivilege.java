package org.lecturestudio.web.portal.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "course_privileges")
public class CoursePrivilege {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	Long id;

	@ManyToOne
	@JoinColumn(name = "privilege_id")
	Privilege privilege;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CoursePrivilege)) {
			return false;
		}

		CoursePrivilege that = (CoursePrivilege) o;

		return Objects.equals(privilege, that.privilege);
	}

	@Override
	public int hashCode() {
		return Objects.hash(privilege);
	}

	@Override
	public String toString() {
		return "CoursePrivilege [name=" + privilege.name + "]";
	}
}