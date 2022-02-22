package org.lecturestudio.web.portal.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
public abstract class CourseFeature {

	@Id
	@SequenceGenerator(name = "CourseFeatureGen", sequenceName = "course_feature_id_seq", allocationSize = 1)
	@GeneratedValue(generator = "CourseFeatureGen")
	private Long id;

	/** The unique service ID number of the service session. */
	@Column(nullable = false)
	private String featureId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	Course course;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	User initiator;


	abstract public String getName();

}
