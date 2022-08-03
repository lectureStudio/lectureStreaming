package org.lecturestudio.web.portal.model;

import javax.persistence.Entity;

@Entity
public class CourseMessageFeature extends CourseFeature {

	@Override
	public String getName() {
		return "chat";
	}

}
