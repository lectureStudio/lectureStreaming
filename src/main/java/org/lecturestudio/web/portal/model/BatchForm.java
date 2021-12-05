package org.lecturestudio.web.portal.model;

import java.util.ArrayList;
import java.util.List;

import org.lecturestudio.web.portal.model.dto.CourseDto;

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
public class BatchForm {
	
	List<CourseDto> courses = new ArrayList<>();

	List<BatchCourse> batches = new ArrayList<>();

}
