package org.lecturestudio.web.portal.service;




import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.Transactional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;


@Service
public class CourseRoleSystemDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final AtomicBoolean done = new AtomicBoolean();

    @Autowired
    private RoleService roleService;

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event){
        if (done.compareAndSet(false, true)) {
            roleService.loadInitialRoleData();
        }
    }
}
