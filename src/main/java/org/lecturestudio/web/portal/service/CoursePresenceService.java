package org.lecturestudio.web.portal.service;

import static java.util.Objects.nonNull;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lecturestudio.web.api.message.CoursePresenceMessage;
import org.lecturestudio.web.api.stream.model.CourseParticipantType;
import org.lecturestudio.web.api.stream.model.CoursePresence;
import org.lecturestudio.web.api.stream.model.CoursePresenceType;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.util.SimpEmitter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class CoursePresenceService {

	private final Map<Pair<String, Long>, CoursePresenceType> presenceTypeMap = new ConcurrentHashMap<>();

	@Autowired
	private CourseService courseService;

	@Autowired
	private UserService userService;

	@Autowired
	private SimpEmitter simpEmitter;

	@Value("${simp.events.presence}")
	private String presenceEvent;


	public void sendCoursePresence(CoursePresence presence, CoursePresenceType presenceType, String userId, Long courseId) {
		User user = userService.findById(userId).orElse(null);

		if (nonNull(user)) {
			sendCoursePresence(presence, presenceType, user, courseId);
		}
	}

	public void sendCoursePresence(CoursePresence presence, CoursePresenceType presenceType, User user, Long courseId) {
		CoursePresenceMessage message = createMessage(presence, presenceType, user, courseId);

		simpEmitter.emmitEvent(courseId, presenceEvent, message);

		if (presence == CoursePresence.CONNECTED) {
			presenceTypeMap.put(Pair.of(user.getUserId(), courseId), presenceType);
		}
		else {
			presenceTypeMap.remove(Pair.of(user.getUserId(), courseId));
		}
	}

	public void sendCoursePresenceToOrganisers(CoursePresence presence, CoursePresenceType presenceType, String userId, Long courseId) {
		User user = userService.findById(userId).orElse(null);

		if (nonNull(user)) {
			CoursePresenceMessage message = createMessage(presence, presenceType, user, courseId);

			for (String uid : courseService.getOrganisators(courseId)) {
				simpEmitter.emmitEventToUser(courseId, presenceEvent, message, uid);
			}
		}
	}

	public CourseParticipantType getCourseParticipantType(User user, Long courseId) {
		for (var registration : user.getRegistrations()) {
			if (registration.getCourse().getId() == courseId) {
				return CourseParticipantType.ORGANISATOR;
			}
		}

		Set<Role> userRoles = new HashSet<>(user.getRoles());
		userRoles.addAll(courseService.findAllRoles(courseId, user.getUserId()));

		CourseParticipantType pType = CourseParticipantType.PARTICIPANT;

		for (Role role : userRoles) {
			if (role.getName().equals("organisator")) {
				pType = CourseParticipantType.ORGANISATOR;
			}
			else if (role.getName().equals("co-organisator")) {
				pType = CourseParticipantType.CO_ORGANISATOR;
			}
		}

		return pType;
	}

	public CoursePresenceType getPresenceType(User user, Long courseId) {
		return presenceTypeMap.getOrDefault(Pair.of(user.getUserId(), courseId), CoursePresenceType.CLASSROOM);
	}

	private CoursePresenceMessage createMessage(CoursePresence presence, CoursePresenceType presenceType, User user, Long courseId) {
		CoursePresenceMessage presenceMessage = new CoursePresenceMessage();
		presenceMessage.setDate(ZonedDateTime.now());
		presenceMessage.setFamilyName(user.getFamilyName());
		presenceMessage.setFirstName(user.getFirstName());
		presenceMessage.setUserId(user.getUserId());
		presenceMessage.setCourseParticipantType(getCourseParticipantType(user, courseId));
		presenceMessage.setCoursePresence(presence);
		presenceMessage.setCoursePresenceType(presenceType);

		return presenceMessage;
	}
}
