package org.lecturestudio.web.portal.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.lecturestudio.web.api.message.WebMessage;

public class CourseFeatureState {

	private final List<CourseFeatureListener> listeners = new CopyOnWriteArrayList<>();


	public void addCourseFeatureListener(CourseFeatureListener listener) {
		if (listeners.contains(listener)) {
			return;
		}

		listeners.add(listener);
	}

	public void removeCourseFeatureListener(CourseFeatureListener listener) {
		listeners.remove(listener);
	}

	public void postCourseFeatureMessage(long courseId, WebMessage message) {
		for (var listener : listeners) {
			listener.onFeatureMessage(courseId, message);
		}
	}

}
