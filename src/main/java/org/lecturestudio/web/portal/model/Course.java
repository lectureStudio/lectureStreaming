package org.lecturestudio.web.portal.model;

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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
@Table(name = "courses")
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	Long id;

	@Column(nullable = false)
	String roomId;

	@Column(nullable = false)
	@Size(min = 3, max = 200)
	String title;

	@Column(columnDefinition = "TEXT")
	String description;

	@Column
	String passcode;

	@Column(nullable = true)
	Boolean isConference;

	@OneToMany(mappedBy = "course", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	Set<CourseRegistration> registrations;

	@OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
	Set<CourseFeature> features;

	@ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	@JoinTable(name = "course_course_roles",
		joinColumns = @JoinColumn(name = "course_id", referencedColumnName = "id"),
		inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	Set<CourseRole> roles;

	@OneToMany(mappedBy = "course", fetch = FetchType.EAGER, orphanRemoval = true, cascade = { CascadeType.ALL })
	@OnDelete(action = OnDeleteAction.CASCADE)
	Set<CourseUserRole> userRoles;

}
