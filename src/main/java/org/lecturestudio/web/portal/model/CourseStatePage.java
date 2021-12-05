package org.lecturestudio.web.portal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.lecturestudio.core.recording.action.PlaybackAction;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseStatePage {

	private int pageNumber;

	@JsonIgnore
	private List<PlaybackAction> actions;

}
