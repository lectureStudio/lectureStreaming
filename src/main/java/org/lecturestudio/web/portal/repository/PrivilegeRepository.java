package org.lecturestudio.web.portal.repository;

import org.lecturestudio.web.portal.model.Privilege;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

	Privilege findByName(String name);

	@Override
	void delete(Privilege role);

}