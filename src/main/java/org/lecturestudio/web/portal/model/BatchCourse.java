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
public class BatchCourse {
	
	long courseId;

	int count;

	
	@Override
	public String toString() {
		return "BatchCourse [count=" + count + ", courseId=" + courseId + "]";
	}
}
