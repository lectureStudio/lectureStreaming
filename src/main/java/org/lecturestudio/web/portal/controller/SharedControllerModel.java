package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import org.lecturestudio.web.portal.saml.LectUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SharedControllerModel {

	@Autowired
	private GitProperties gitProperties;


	@ModelAttribute("appName")
	String appName() {
		return "lectureStreaming";
	}

	@ModelAttribute("appVersion")
	String appVersion() {
		return gitProperties.get("build.version").replace("-SNAPSHOT", "") + "-" + gitProperties.get("total.commit.count");
	}

	@ModelAttribute("user")
	LectUserDetails userDetails(Authentication authentication) {
		return nonNull(authentication) ? (LectUserDetails) authentication.getDetails() : null;
	}
}
