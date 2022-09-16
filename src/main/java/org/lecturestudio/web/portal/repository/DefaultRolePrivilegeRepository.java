package org.lecturestudio.web.portal.repository;

import java.util.List;

import org.lecturestudio.web.portal.model.DefaultRolePrivilege;
import org.lecturestudio.web.portal.model.DefaultRolePrivilegeId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefaultRolePrivilegeRepository extends JpaRepository<DefaultRolePrivilege, DefaultRolePrivilegeId> {

	List<DefaultRolePrivilege> findAllByRoleId(Long roleId);

}