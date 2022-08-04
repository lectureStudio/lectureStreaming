package org.lecturestudio.web.portal.config;

import org.lecturestudio.web.portal.security.CoursePrivilegeEvaluator;
import org.lecturestudio.web.portal.security.CoursePrivilegeMethodExpressionHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

	@Bean
	public CoursePrivilegeEvaluator coursePrivilegeEvaluator() {
		return new CoursePrivilegeEvaluator();
	}

	@Override
	protected MethodSecurityExpressionHandler createExpressionHandler() {
		CoursePrivilegeMethodExpressionHandler expressionHandler = new CoursePrivilegeMethodExpressionHandler();
		expressionHandler.setPermissionEvaluator(coursePrivilegeEvaluator());

		return expressionHandler;
	}
}
