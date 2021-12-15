package org.lecturestudio.web.portal.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseConnectionRequest;

@Repository
public interface CourseConnectionRequestRepository extends CrudRepository<CourseConnectionRequest, Long> {

    Optional<CourseConnectionRequest> findByRequestId(@Param("requestId") Long requestId);

    @Query("SELECT c FROM CourseConnectionRequest c WHERE c.courseId = ?1")
    List<CourseConnectionRequest> findUsersByCourseId(Long courseId);

    @Query("SELECT COUNT(*) FROM CourseConnectionRequest c WHERE c.userId = ?1 AND c.courseId = ?2")
    Long findNumRequestsToCourseOfUser(String userId, Long courseId);
}
