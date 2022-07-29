package org.lecturestudio.web.portal.service;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.Transactional;

import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.repository.PrivilegeRepository;
import org.lecturestudio.web.portal.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
public class RoleSystemLoader implements ApplicationListener<ContextRefreshedEvent> {

	private static final Role[] ROLES = {
			new Role(null, "organisator", "role.organisator"),
			new Role(null, "co-organisator", "role.co-organisator"),
			new Role(null, "participant", "role.participant")
	};

	private static final Privilege[] PRIVILEGES = {
			new Privilege(null, "COURSE_ALTER_PRIVILEGES", "privilege.course.alter.privileges"),
			new Privilege(null, "COURSE_EDIT", "privilege.course.edit"),
			new Privilege(null, "COURSE_DELETE", "privilege.course.delete"),
			new Privilege(null, "CHAT_READ", "privilege.chat.read"),
			new Privilege(null, "CHAT_WRITE", "privilege.chat.write"),
			new Privilege(null, "CHAT_WRITE_TO_ORGANISATOR", "privilege.chat.write.to.organisator"),
			new Privilege(null, "CHAT_WRITE_PRIVATELY", "privilege.chat.write.privately"),
			new Privilege(null, "QUIZ_PARTICIPATION", "privilege.quiz.participation"),
			new Privilege(null, "SPEECH", "privilege.speech")
	};

	private final AtomicBoolean done = new AtomicBoolean();

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PrivilegeRepository privilegeRepository;


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (done.compareAndSet(false, true)) {
			for (Role role : ROLES) {
				createRoleIfNotFound(role);
			}

			for (Privilege privilege : PRIVILEGES) {
				privilege = createPrivilegeIfNotFound(privilege);
			}
		}
	}

	@Transactional
	Privilege createPrivilegeIfNotFound(Privilege privilege) {
		Privilege foundPrivilege = privilegeRepository.findByName(privilege.getName());

		if (foundPrivilege == null) {
			foundPrivilege = privilegeRepository.save(privilege);
		}

		return foundPrivilege;
	}

	@Transactional
	Role createRoleIfNotFound(Role role) {
		Role foundRole = roleRepository.findByName(role.getName());

		if (foundRole == null) {
			foundRole = roleRepository.save(role);
		}

		return role;
	}
}