package org.lecturestudio.web.portal.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
public class CourseRoleSystemDataLoaderService implements ApplicationListener<ContextRefreshedEvent> {

	private final AtomicBoolean done = new AtomicBoolean();

	@Autowired
	private RoleService roleService;


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (done.compareAndSet(false, true)) {
			roleService.loadInitialData();
		}
	}
}