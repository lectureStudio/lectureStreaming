package org.lecturestudio.web.portal.model.dto;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.lecturestudio.web.portal.model.CourseMediaState;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.model.CourseStateDocument;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseStateDto {

	Long courseId;

	String userId;

	Long timeStarted;

	String title;

	String description;

	CourseMessageFeature messageFeature;

	CourseQuizFeature quizFeature;

	boolean isConference;

	boolean isProtected;

	boolean isRecorded;

	Map<Long, CourseStateDocument> documentMap;

	CourseStateDocument activeDocument;

	Set<CoursePrivilegeDto> userPrivileges;

	CourseMediaState mediaState;

}
