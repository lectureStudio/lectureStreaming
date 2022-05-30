package org.lecturestudio.web.portal.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.lecturestudio.web.api.model.quiz.Quiz.QuizType;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CourseQuizFeature extends CourseFeature {

	@Column(columnDefinition = "TEXT")
	String question;

	QuizType type;

	@ElementCollection
	List<String> options;

	@ElementCollection
	List<String> users;

	@OneToMany(mappedBy = "feature", cascade = { CascadeType.ALL })
	List<CourseQuizResource> resources;


	@Override
	public String getName() {
		return "quiz";
	}
}
