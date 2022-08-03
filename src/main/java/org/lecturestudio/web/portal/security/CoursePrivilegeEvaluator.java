package org.lecturestudio.web.portal.security;

import java.io.Serializable;

import javax.annotation.Resource;

import org.lecturestudio.web.portal.model.ScopedCoursePrivileges;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

public class CoursePrivilegeEvaluator implements PermissionEvaluator {

	@Resource(name = "scopedCoursePrivileges")
	private ScopedCoursePrivileges requestScopedPrivileges;


	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		System.out.println("hasPermission: " + requestScopedPrivileges.getPrivileges());
		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		System.out.println("hasPermission: " + requestScopedPrivileges.getPrivileges());
		return false;
	}

}
