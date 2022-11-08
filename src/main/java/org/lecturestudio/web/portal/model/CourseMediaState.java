package org.lecturestudio.web.portal.model;

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
public class CourseMediaState {

	private boolean microphoneActive;

	private boolean cameraActive;

	private boolean screenActive;

}
