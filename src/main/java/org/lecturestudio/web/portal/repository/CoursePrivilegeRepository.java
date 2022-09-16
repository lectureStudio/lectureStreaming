package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.CoursePrivilege;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursePrivilegeRepository extends JpaRepository<CoursePrivilege, Long> {

	@Override
	void delete(CoursePrivilege privilege);

}