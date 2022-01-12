package org.lecturestudio.web.portal.model;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CourseStates {

	private final Map<Long, CourseState> states = new ConcurrentHashMap<>();

	private final List<CourseStateListener> listeners = new CopyOnWriteArrayList<>();


	public void setCourseState(Long courseId, CourseState state) {
		requireNonNull(courseId);
		requireNonNull(state);

		states.put(courseId, state);

		notifyCourseStarted(courseId, state);
	}

	public void removeCourseState(Long courseId) {
		if (nonNull(courseId)) {
			CourseState state = states.remove(courseId);

			notifyCourseEnded(courseId, state);
		}
	}

	public CourseState getCourseState(Long courseId) {
		return states.get(courseId);
	}

	public boolean hasCourseState(Long courseId) {
		return states.containsKey(courseId);
	}

	public void removeSessionId(BigInteger sessionId) {
		for (CourseState state : states.values()) {
			state.removeParticipantWithSessionId(sessionId);
		}
	}

	public void addCourseStateListener(CourseStateListener listener) {
		if (listeners.contains(listener)) {
			return;
		}

		listeners.add(listener);
	}

	public void removeCourseStateListener(CourseStateListener listener) {
		listeners.remove(listener);
	}

	public void notifyCourseStarted(long courseId, CourseState state) {
		for (var listener : listeners) {
			listener.courseStarted(courseId, state);
		}
	}

	public void notifyCourseEnded(long courseId, CourseState state) {
		for (var listener : listeners) {
			listener.courseEnded(courseId, state);
		}
	}
}
