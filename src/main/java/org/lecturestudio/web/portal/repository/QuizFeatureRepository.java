package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseQuizFeature;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizFeatureRepository extends CrudRepository<CourseQuizFeature, Long> {

	Optional<CourseQuizFeature> findByCourseId(@Param("courseId") Long courseId);

}
