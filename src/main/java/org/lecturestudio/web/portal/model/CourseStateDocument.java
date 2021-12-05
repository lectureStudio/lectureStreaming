package org.lecturestudio.web.portal.model;

import java.util.Map;

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
public class CourseStateDocument {

	private long documentId;

	private String documentName;

	private String documentFile;

	private String type;

	private CourseStatePage activePage;

	private Map<Integer, CourseStatePage> pages;

}
