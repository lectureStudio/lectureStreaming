package org.lecturestudio.web.portal.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "course_participants")
@IdClass(CourseUserId.class)
public class CourseParticipant {

	@Id
	Long courseId;

	@Id
	String userId;

	@Column(name = "sessionId", updatable = false, nullable = false)
	String sessionId;

}
