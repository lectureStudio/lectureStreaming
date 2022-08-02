package org.lecturestudio.web.portal.saml;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.lecturestudio.web.portal.config.WebSecurityConfig;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.RoleService;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class LectSAMLUserDetailsService implements SAMLUserDetailsService {

	private final UserService userService;

	private final RoleService roleService;


	public LectSAMLUserDetailsService(UserService userService, RoleService roleService) {
		this.userService = userService;
		this.roleService = roleService;
	}

	@Override
	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
		String firstName = null;
		String familyName = null;
		String username = null;
		Set<GrantedAuthority> authorities = new HashSet<>();
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
				GrantedAuthority member = new SimpleGrantedAuthority("ROLE_MEMBER");
				GrantedAuthority student = new SimpleGrantedAuthority("ROLE_STUDENT");

				for (String affiliation : credential.getAttributeAsStringArray(name)) {
					// Map person affiliations to roles. Higher roles go first.
					switch (affiliation) {
						case "faculty":
						case "employee":
						case "member":
							authorities.add(member);
							break;

						case "student":
							authorities.add(student);
							break;
					}
				}

				if (authorities.isEmpty()) {
					authorities.add(student);
				}
				else if (authorities.contains(student)) {
					// Students may have the "MEMBER" affiliation.
					authorities.remove(member);
				}
			}
		}

		if (isNull(username) || isNull(firstName) || isNull(familyName)) {
			throw new UsernameNotFoundException("Cannot locate user");
		}

		Optional<User> userOpt = userService.findById(username);

		if (userOpt.isEmpty()) {
			// Generate anonymous user-id.
			UUID uuid = UUID.randomUUID();

			while (userService.hasUser(uuid)) {
				uuid = UUID.randomUUID();
			}

			Role defaultRole = roleService.findByName("participant");

			userService.saveUser(User.builder()
				.userId(username)
				.anonymousUserId(uuid)
				.firstName(firstName)
				.familyName(familyName)
				.roles(nonNull(defaultRole) ? Set.of(defaultRole) : Set.of())
				.build());
		}

		return new LectUserDetails(username, firstName, familyName, authorities);
	}

}
