package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import java.util.List;

import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.janus.JanusCoreEvent;
import org.lecturestudio.web.portal.model.janus.JanusCoreEventType;
import org.lecturestudio.web.portal.model.janus.JanusEvent;
import org.lecturestudio.web.portal.model.janus.JanusVideoRoomEvent;
import org.lecturestudio.web.portal.model.janus.JanusVideoRoomEventType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/janus")
public class JanusEventController {

	@Autowired
	private CourseStates courseStates;


	@PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void events(@RequestBody List<JanusEvent> events) {
		for (JanusEvent event : events) {
			if (nonNull(event)) {
				if (event instanceof JanusVideoRoomEvent) {
					JanusVideoRoomEvent roomEvent = (JanusVideoRoomEvent) event;

					System.out.println(roomEvent.getEventType());

					if (roomEvent.getEventType() == JanusVideoRoomEventType.SUBSCRIBED) {
						long courseId = roomEvent.getRoomId().longValue();
						CourseState state = courseStates.getCourseState(courseId);

						if (nonNull(state)) {
							if (nonNull(roomEvent.getOpaqueId())) {
								String participantId = roomEvent.getOpaqueId();

								state.setParticipantSession(participantId, roomEvent.getSessionId());
							}
						}
					}
					else if (roomEvent.getEventType() == JanusVideoRoomEventType.UNSUBSCRIBED) {
						long courseId = roomEvent.getRoomId().longValue();
						CourseState state = courseStates.getCourseState(courseId);

						if (nonNull(state)) {
							state.removeParticipantWithSessionId(roomEvent.getSessionId());
						}
					}
				}
				else if (event instanceof JanusCoreEvent) {
					JanusCoreEvent coreEvent = (JanusCoreEvent) event;

					if (coreEvent.getEventType() == JanusCoreEventType.DESTROYED) {
						courseStates.removeSessionId(coreEvent.getSessionId());
					}
				}
			}
		}
	}
}
