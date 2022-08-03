package org.lecturestudio.web.portal.config;

import org.lecturestudio.web.portal.interceptor.VerifyAccessInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Autowired
	private VerifyAccessInterceptor verifyAccessInterceptor;


	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(verifyAccessInterceptor).addPathPatterns("/course/*");
	}

}
