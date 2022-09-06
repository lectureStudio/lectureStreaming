package org.lecturestudio.web.portal.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.Transactional;

import org.lecturestudio.web.portal.model.DefaultRolePrivilege;
import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.repository.DefaultRolePrivilegeRepository;
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
			new Privilege(null, "COURSE_ALTER_PRIVILEGES", "privilege.course.alter.privileges", null),
			new Privilege(null, "COURSE_EDIT", "privilege.course.edit", null),
			new Privilege(null, "COURSE_DELETE", "privilege.course.delete", null),
			new Privilege(null, "COURSE_STREAM", "privilege.course.stream", null),
			new Privilege(null, "CHAT_READ", "privilege.chat.read", null),
			new Privilege(null, "CHAT_WRITE", "privilege.chat.write", null),
			new Privilege(null, "CHAT_WRITE_TO_ORGANISATOR", "privilege.chat.write.to.organisator", null),
			new Privilege(null, "CHAT_WRITE_PRIVATELY", "privilege.chat.write.privately", null),
			new Privilege(null, "PARTICIPANTS_VIEW", "privilege.participants.view", null),
			new Privilege(null, "QUIZ_PARTICIPATION", "privilege.quiz.participation", null),
			new Privilege(null, "SPEECH", "privilege.speech", null),
	};

	private final AtomicBoolean done = new AtomicBoolean();

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PrivilegeRepository privilegeRepository;

	@Autowired
	private DefaultRolePrivilegeRepository defaultRolePrivilegeRepository;


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (done.compareAndSet(false, true)) {
			for (Role role : ROLES) {
				createRoleIfNotFound(role);
			}

			for (Privilege privilege : PRIVILEGES) {
				createPrivilegeIfNotFound(privilege);
			}

			createPrivilegeDependencies();

			createDefaultRolePrivileges(roleRepository.findByName("organisator"),
					List.of());
			createDefaultRolePrivileges(roleRepository.findByName("co-organisator"),
					List.of("COURSE_ALTER_PRIVILEGES", "COURSE_EDIT", "COURSE_DELETE"));
			createDefaultRolePrivileges(roleRepository.findByName("participant"),
					List.of("COURSE_ALTER_PRIVILEGES", "COURSE_EDIT", "COURSE_DELETE", "COURSE_STREAM", "CHAT_WRITE_PRIVATELY"));
		}
	}

	@Transactional
	void createPrivilegeDependencies() {
		Privilege cwp = privilegeRepository.findByName("CHAT_WRITE_PRIVATELY");
		cwp.setDependencies(new HashSet<>());
		cwp.getDependencies().add(privilegeRepository.findByName("PARTICIPANTS_VIEW"));

		privilegeRepository.save(cwp);
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

	@Transactional
	void createDefaultRolePrivileges(Role role, List<String> disabledPrivileges) {
		List<Privilege> privileges = privilegeRepository.findAll();
		List<DefaultRolePrivilege> defaultPrivileges = new ArrayList<>();

		for (Privilege privilege : privileges) {
			defaultPrivileges.add(DefaultRolePrivilege.builder()
					.role(role)
					.privilege(privilege)
					.enabled(!disabledPrivileges.contains(privilege.getName()))
					.build());
		}

		defaultRolePrivilegeRepository.saveAll(defaultPrivileges);
	}
}