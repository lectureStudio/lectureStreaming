package org.lecturestudio.web.portal.model.dto;

import java.util.List;

import org.lecturestudio.web.api.model.quiz.Quiz.QuizType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class CourseQuizFeatureDto extends CourseFeatureDto {

	String question;

	QuizType type;

	List<String> options;

}
