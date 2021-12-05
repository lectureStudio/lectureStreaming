package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseSpeechRequest;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSpeechRequestRepository extends CrudRepository<CourseSpeechRequest, Long> {

	Optional<CourseSpeechRequest> findByRequestId(@Param("requestId") Long requestId);

}
