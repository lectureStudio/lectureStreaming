package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.model.CourseParticipantId;
import org.lecturestudio.web.portal.model.User;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseParticipantRepository extends CrudRepository<CourseParticipant, CourseParticipantId> {

	public void deleteBySessionId(String sessionId);

	public Optional<CourseParticipant> findBySessionId(String sessionId);

	@Query("SELECT CASE WHEN count(cp) > 0 THEN true ELSE false END FROM CourseParticipant cp WHERE cp.user.userId = ?1")
	public boolean existsByUser(String userId);

	@Query(value = "SELECT user FROM CourseParticipant cp WHERE cp.courseId = ?1")
	public Iterable<User> findAllUsersByCourseId(Long courseId, Sort sort);

	@Query("SELECT COUNT(cp) FROM CourseParticipant cp WHERE cp.courseId = ?1")
	public Long getNumOfUsersByCourseId(Long courseId);

}
