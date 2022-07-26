package org.lecturestudio.web.portal.saml;

import static java.util.Objects.isNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
		Set<String> roles = new HashSet<>();
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
						roles.add("lecturer");
					}
					else if (affiliation.equals("employee")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
						roles.add("lecturer");
					}
					else if (affiliation.equals("member")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
						roles.add("lecturer");
					}
					else if (affiliation.equals("affiliate")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_AFFILIATE"));
						roles.add("assistant");
					}
					else if (affiliation.equals("student")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
						roles.add("student");
					}
				}
			}
		}

		if (isNull(username) || isNull(firstName) || isNull(familyName)) {
			throw new UsernameNotFoundException("Cannot locate user");
		}

		Optional<User> userOpt = userService.findById(username);

		Set<Role> userRoles = roles.stream()
			.filter((r) -> {
				return roleService.existsByName(r);
			})
			.map((r) -> {
				return roleService.findRoleByName(r).get();
			})
			.collect(Collectors.toSet());

		if (userOpt.isEmpty()) {
			UUID uuid = UUID.randomUUID();

			while (userService.hasUser(uuid)) {
				uuid = UUID.randomUUID();
			}

			userService.saveUser(User.builder()
				.userId(username)
				.anonymousUserId(uuid)
				.firstName(firstName)
				.familyName(familyName)
				.roles(userRoles)
				.build());
		}
		else {
			User user = userOpt.get();
			user.setRoles(userRoles);

			userService.saveUser(user);
		}

		return new LectUserDetails(username, firstName, familyName, authorities);
	}

}
