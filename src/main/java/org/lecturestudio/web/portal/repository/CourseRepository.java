package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.Course;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends PagingAndSortingRepository<Course, Long> {

}
