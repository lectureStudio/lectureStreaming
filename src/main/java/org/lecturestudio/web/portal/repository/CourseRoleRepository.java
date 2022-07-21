package org.lecturestudio.web.portal.repository;

import java.util.Set;

import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.CourseRoleId;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRoleRepository extends CrudRepository<CourseRole, CourseRoleId> {

    public int deleteCourseRoleByCourseId(long id);

    public Set<CourseRole> findByCourseId(Long courseId);

}