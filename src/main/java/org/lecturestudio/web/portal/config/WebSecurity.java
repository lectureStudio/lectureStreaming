package org.lecturestudio.web.portal.config;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.lecturestudio.web.portal.exception.CourseNotFoundException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.ScopedCoursePrivileges;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.CourseService;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class WebSecurity {

	@Resource(name = "scopedCoursePrivileges")
	private ScopedCoursePrivileges requestScopedPrivileges;

	@Autowired
	private CourseService courseService;

	@Autowired
	private UserService userService;


	public boolean checkCourseId(Authentication authentication, Long courseId) {
		System.out.println("checkCourseId: " + courseId + " " + requestScopedPrivileges);

		try {
			Course course = courseService.findById(courseId)
					.orElseThrow(() -> new CourseNotFoundException());

			User user = userService.findById(authentication.getName())
					.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

			Set<CourseRole> courseRoles = course.getRoles();
			Set<Role> userRoles = user.getRoles();

			Set<Privilege> userPrivileges = new HashSet<>();

			for (CourseRole courseRole : courseRoles) {
				if (userRoles.contains(courseRole.getRole())) {
					for (CoursePrivilege coursePrivilege : courseRole.getPrivileges()) {
						Privilege privilege = coursePrivilege.getPrivilege();

						userPrivileges.add(privilege);
					}
				}
			}

			requestScopedPrivileges.setPrivileges(userPrivileges);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

}
