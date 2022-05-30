package org.lecturestudio.web.portal.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CourseQuizResource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	Long id;

	@Column(name = "course_id")
	Long courseId;

	@Column(name = "name")
	String name;

	@Column(name = "type")
	String type;

	@Lob
	@Type(type = "org.hibernate.type.ImageType")
	byte[] content;

	@ManyToOne
	@JoinColumn(name = "feature_id")
	CourseQuizFeature feature;

}
