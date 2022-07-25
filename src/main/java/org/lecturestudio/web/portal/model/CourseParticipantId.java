package org.lecturestudio.web.portal.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseParticipantId implements Serializable {
	
	private String sessionId;

	private String user;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CourseParticipantId)) {
			return false;
		}

		CourseParticipantId that = (CourseParticipantId) o;

		return Objects.equals(sessionId, that.sessionId) && Objects.equals(user, that.user);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sessionId, user);
	}

}
