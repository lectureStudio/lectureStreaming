package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import java.time.ZonedDateTime;

import org.apache.commons.lang.RandomStringUtils;

import org.lecturestudio.web.portal.model.PersonalToken;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.PersonalTokenService;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PersonalTokenController {

	@Autowired
	private UserService userService;

	@Autowired
	private PersonalTokenService tokenService;


	@PostMapping("/token/new")
	public String newToken(Authentication authentication, RedirectAttributes redirectAttrs) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		User user = userService.findById(details.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		String token = RandomStringUtils.random(32, true, true);

		PersonalToken personalToken = user.getToken();

		if (nonNull(personalToken)) {
			personalToken.setToken(token);
			personalToken.setDateCreated(ZonedDateTime.now());
			personalToken.setDateLastUsed(null);

			tokenService.saveToken(personalToken);
		}
		else {
			personalToken = PersonalToken.builder()
				.token(token)
				.user(user)
				.build();

			user.setToken(personalToken);

			userService.saveUser(user);
		}

		redirectAttrs.addFlashAttribute("generated", true);

		return "redirect:/settings";
	}

	@PostMapping("/token/delete")
	public String deleteToken(Authentication authentication, Model model) {
		LectUserDetails details = (LectUserDetails) authentication.getDetails();

		User user = userService.findById(details.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		PersonalToken personalToken = user.getToken();

		if (nonNull(personalToken)) {
			user.setToken(null);

			userService.saveUser(user);

			tokenService.deleteToken(personalToken);
		}

		return "redirect:/settings";
	}
}
