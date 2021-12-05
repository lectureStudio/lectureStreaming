package org.lecturestudio.web.portal.saml;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class LectUserDetails extends User {

	private final String firstName;

	private final String familyName;


	public LectUserDetails(String username, String firstName, String familyName, Collection<? extends GrantedAuthority> authorities) {
		super(username, "***", authorities);

		this.firstName = firstName;
		this.familyName = familyName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getFamilyName() {
		return familyName;
	}
}
