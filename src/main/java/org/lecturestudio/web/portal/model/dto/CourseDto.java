package org.lecturestudio.web.portal.model.dto;

import static java.util.Objects.nonNull;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseDto {

	Long id;

	String userId;

	String roomId;

	Long createdTimestamp;

	String title;

	String description;

	String url;

	List<UserDto> authors;

	CourseMessageFeature messageFeature;

	CourseQuizFeature quizFeature;

	boolean conference;

	boolean isProtected;

	boolean isLive;

	boolean isRecorded;

	boolean canEdit;

	boolean canDelete;


	public boolean hasMessenger() {
		return nonNull(messageFeature);
	}

	public boolean hasQuiz() {
		return nonNull(quizFeature);
	}

	public boolean hasFeatures() {
		return hasMessenger() || hasQuiz();
	}
}
