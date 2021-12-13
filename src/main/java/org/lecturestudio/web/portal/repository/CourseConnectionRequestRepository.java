package org.lecturestudio.web.portal.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseConnectionRequest;

@Repository
public interface CourseConnectionRequestRepository extends CrudRepository<CourseConnectionRequest, Long> {

    Optional<CourseConnectionRequest> findByRequestId(@Param("requestId") Long requestId);

    List<CourseConnectionRequest> findByCourseId(@Param("courseId") Long courseId);
}
