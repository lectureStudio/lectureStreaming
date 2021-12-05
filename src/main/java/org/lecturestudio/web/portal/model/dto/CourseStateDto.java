package org.lecturestudio.web.portal.model.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.lecturestudio.web.portal.model.CourseStateDocument;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseStateDto {

	Map<Long, CourseStateDocument> documentMap;

	CourseStateDocument avtiveDocument;

}
