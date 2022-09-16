package org.lecturestudio.web.portal.repository;

import java.util.List;

import org.lecturestudio.web.portal.model.CourseUserRole;
import org.lecturestudio.web.portal.model.CourseUserRoleId;
import org.lecturestudio.web.portal.model.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseUserRoleRepository extends JpaRepository<CourseUserRole, Long> {

	@Query(value = "SELECT role FROM CourseUserRole cur WHERE cur.course.id = ?1 AND cur.userId = ?2")
	List<Role> findAllRoles(Long courseId, String username);

}
