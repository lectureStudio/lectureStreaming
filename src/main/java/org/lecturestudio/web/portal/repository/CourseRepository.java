package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.Course;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends PagingAndSortingRepository<Course, Long> {

	@Query(value = "SELECT c.id, c.room_id, c.title, c.description, c.passcode FROM courses c LEFT JOIN course_registration r ON r.course_id = c.id WHERE r.user_id = :userId", nativeQuery = true)
	Iterable<Course> findAllByUserId(@Param("userId") String userId);

}
