package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.CourseRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRoleRepository extends JpaRepository<CourseRole, Long> {

	@Override
	void delete(CourseRole role);

}