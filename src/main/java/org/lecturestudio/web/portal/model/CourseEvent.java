package org.lecturestudio.web.portal.model;

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
public class CourseEvent {

	Long courseId;

	Long createdTimestamp;

	Boolean hasFeatures;

	Boolean started;

}
