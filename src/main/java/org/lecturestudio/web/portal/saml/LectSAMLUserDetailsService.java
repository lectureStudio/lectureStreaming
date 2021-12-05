package org.lecturestudio.web.portal.saml;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.lecturestudio.web.portal.config.WebSecurityConfig;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class LectSAMLUserDetailsService implements SAMLUserDetailsService {

	private final UserService userService;


	public LectSAMLUserDetailsService(UserService userService) {
		this.userService = userService;
	}

	@Override
	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
		String firstName = null;
		String familyName = null;
		String username = null;
		List<GrantedAuthority> authorities = new ArrayList<>();
		Map<String, String> attributeMap = WebSecurityConfig.SSO_REQUESTED_ATTRIBUTES;

		for (var attr : credential.getAttributes()) {
			String name = attr.getName();

			if (name.equals(attributeMap.get("givenName"))) {
				firstName = credential.getAttributeAsString(name);
			}
			else if (name.equals(attributeMap.get("surname"))) {
				familyName = credential.getAttributeAsString(name);
			}
			else if (name.equals(attributeMap.get("cn"))) {
				username = credential.getAttributeAsString(name);
			}
			else if (name.equals(attributeMap.get("eduPersonAffiliation"))) {
				for (String affiliation : credential.getAttributeAsStringArray(name)) {
					// Map person affiliations to roles. Higher roles go first.
					if (affiliation.equals("faculty")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_FACULTY"));
					}
					else if (affiliation.equals("employee")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
					}
					else if (affiliation.equals("member")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
					}
					else if (affiliation.equals("affiliate")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_AFFILIATE"));
					}
				}
			}
		}

		if (isNull(username) || isNull(firstName) || isNull(familyName)) {
			throw new UsernameNotFoundException("Cannot locate user");
		}

		Optional<User> userOpt = userService.findById(username);

		if (userOpt.isEmpty()) {
			userService.saveUser(User.builder()
				.userId(username)
				.firstName(firstName)
				.familyName(familyName)
				.build());
		}

		return new LectUserDetails(username, firstName, familyName, authorities);
	}

}
