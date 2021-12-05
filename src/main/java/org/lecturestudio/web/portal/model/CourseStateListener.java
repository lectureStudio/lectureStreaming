package org.lecturestudio.web.portal.model;

public interface CourseStateListener {

	void courseStarted(long courseId, CourseState state);

	void courseEnded(long courseId, CourseState state);

}
