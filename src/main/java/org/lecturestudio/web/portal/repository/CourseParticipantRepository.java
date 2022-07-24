package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.model.CourseUserId;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseParticipantRepository extends CrudRepository<CourseParticipant, CourseUserId> {

	public void deleteBySessionId(String sessionId);

	public Optional<CourseParticipant> findBySessionId(String sessionId);

}
