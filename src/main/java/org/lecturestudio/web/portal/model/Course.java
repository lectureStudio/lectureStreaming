package org.lecturestudio.web.portal.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;

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
	@Size(min = 3, max = 50)
	String title;

	@Column(columnDefinition = "TEXT")
	String description;

	@Column
	String passcode;

	@OneToMany(mappedBy = "course", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	Set<CourseRegistration> registrations = new HashSet<>();

	@OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
	Set<CourseFeature> features;

}
