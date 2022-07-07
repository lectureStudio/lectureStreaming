package org.lecturestudio.web.portal.model;

import org.lecturestudio.web.portal.model.dto.CourseFeatureDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseFeatureEvent {

	Long courseId;

	Boolean started;

	CourseFeatureDto feature;

}
