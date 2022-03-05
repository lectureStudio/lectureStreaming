package org.lecturestudio.web.portal.repository;

import java.util.Optional;

import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseRegistration;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface CourseRegistrationRepository extends Repository<CourseRegistration, Long> {

	@Query(value = "SELECT * FROM course_registration r WHERE r.user_id = :userId AND r.course_id = :courseId", nativeQuery = true)
	Optional<CourseRegistration> findByCourseAndUserId(@Param("courseId") Long courseId, @Param("userId") String userId);

	Optional<CourseRegistration> findByCourse(Course course);

}
