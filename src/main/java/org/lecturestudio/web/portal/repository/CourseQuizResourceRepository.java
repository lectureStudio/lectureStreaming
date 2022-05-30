package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseQuizResource;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseQuizResourceRepository extends CrudRepository<CourseQuizResource, Long> {

	Optional<CourseQuizResource> findByCourseIdAndName(long courseId, String name);

}
