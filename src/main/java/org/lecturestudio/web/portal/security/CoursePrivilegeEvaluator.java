package org.lecturestudio.web.portal.security;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.util.Set;

import javax.annotation.Resource;

import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.ScopedCoursePrivileges;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

public class CoursePrivilegeEvaluator implements PermissionEvaluator {

	@Resource(name = "scopedCoursePrivileges")
	private ScopedCoursePrivileges requestScopedPrivileges;


	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		// System.out.println("hasPermission: " + permission + " -> " + requestScopedPrivileges.getPrivileges());

		final Set<Privilege> privileges = requestScopedPrivileges.getPrivileges();

		if (isNull(authentication) || isNull(privileges) || !(permission instanceof String)) {
			return false;
		}

		for (Privilege privilege : privileges) {
			if (privilege.getName().equals(permission)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		return false;
	}

}
