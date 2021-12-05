package org.lecturestudio.web.portal.model;

import org.lecturestudio.web.api.message.WebMessage;

public interface CourseFeatureListener {

	void onFeatureMessage(long courseId, WebMessage message);

}
