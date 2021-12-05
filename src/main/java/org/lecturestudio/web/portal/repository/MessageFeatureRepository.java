package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseMessageFeature;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageFeatureRepository extends CrudRepository<CourseMessageFeature, Long> {

	Optional<CourseMessageFeature> findByCourseId(@Param("courseId") Long courseId);

}
