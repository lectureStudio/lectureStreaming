package org.lecturestudio.web.portal.config;

import java.util.Set;

import javax.annotation.Resource;

import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.ScopedCoursePrivileges;
import org.lecturestudio.web.portal.service.CourseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WebSecurity {

	@Resource(name = "scopedCoursePrivileges")
	private ScopedCoursePrivileges requestScopedPrivileges;

	@Autowired
	private CourseService courseService;


	public boolean checkCourseId(Authentication authentication, Long courseId) {
		// System.out.printf("checkCourseId: %d, %s%n", courseId, authentication.getName());

		try {
			final Set<Privilege> userPrivileges = courseService.getUserPrivileges(courseId, authentication.getName());

			requestScopedPrivileges.setPrivileges(userPrivileges);
		}
		catch (Exception e) {
			return false;
		}

		return true;
	}

}
