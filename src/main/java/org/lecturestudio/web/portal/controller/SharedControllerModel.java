package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import org.lecturestudio.web.portal.saml.LectUserDetails;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SharedControllerModel {

	@ModelAttribute("appName")
	String appName() {
		return "lectureStreaming";
	}

	@ModelAttribute("user")
	LectUserDetails userDetails(Authentication authentication) {
		return nonNull(authentication) ? (LectUserDetails) authentication.getDetails() : null;
	}

}
