package org.lecturestudio.web.portal.security;

import java.time.ZonedDateTime;
import java.util.Set;

import org.lecturestudio.web.portal.model.PersonalToken;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.service.PersonalTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class TokenAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

	@Autowired
	PersonalTokenService tokenService;


	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
		// Nothing to do here.
	}

	@Override
	protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authenticationToken)
			throws AuthenticationException {
		Object token = authenticationToken.getCredentials();
		PersonalToken personalToken = tokenService.findByToken(String.valueOf(token))
			.orElseThrow(() -> new UsernameNotFoundException("Cannot find user with authentication token"));

		personalToken.setDateLastUsed(ZonedDateTime.now());
		tokenService.saveToken(personalToken);

		User user = personalToken.getUser();

		return new LectUserDetails(user.getUserId(), user.getFirstName(), user.getFamilyName(), Set.of());
	}

}
