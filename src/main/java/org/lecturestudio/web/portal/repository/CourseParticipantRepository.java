package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.model.CourseUserId;
import org.lecturestudio.web.portal.model.User;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseParticipantRepository extends CrudRepository<CourseParticipant, CourseUserId> {

	public void deleteBySessionId(String sessionId);

	public Optional<CourseParticipant> findBySessionId(String sessionId);

	@Query(value = "SELECT user FROM CourseParticipant cp WHERE cp.courseId = ?1")
	public Iterable<User> findAllUsersByCourseId(Long courseId);

}
