package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.CourseUser;
import org.lecturestudio.web.portal.model.CourseUserId;
import org.springframework.data.repository.CrudRepository;

public interface CourseUserRepository extends CrudRepository<CourseUser, CourseUserId>{

    public int deleteCourseUserByCourseId(Long courseId);

    public int deleteCourseUserByUserId(String userId);

    public Iterable<CourseUser> findByCourseId(Long courseId);
        
}
