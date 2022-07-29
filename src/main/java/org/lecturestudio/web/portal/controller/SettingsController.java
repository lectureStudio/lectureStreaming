package org.lecturestudio.web.portal.controller;

import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	@Autowired
	private UserService userService;


	@RequestMapping()
	public String settingsIndex(Authentication authentication, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();
		User user = userService.findById(details.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		model.addAttribute("token", user.getToken());

		return "settings";
	}

}
