package org.lecturestudio.web.portal.interceptor;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VerifyAccessInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		// System.out.println();
		// System.out.println(auth);
		// System.out.println();

		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("WRITE_PRIVILEGE"));

		var newAuth = new ExpiringUsernameAuthenticationToken(null, auth.getPrincipal(), auth.getCredentials(), authorities);
		newAuth.setDetails(auth.getDetails());

		// SecurityContextHolder.getContext().setAuthentication(newAuth);

		return true;
	}

}
