package org.lecturestudio.web.portal.keycloak;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OidcKeycloakAccount;

import org.lecturestudio.web.portal.saml.LectUserDetails;

import org.springframework.security.core.GrantedAuthority;

public class KeycloakUserDetailsAccount extends LectUserDetails implements OidcKeycloakAccount {

    private final OidcKeycloakAccount account;


    public KeycloakUserDetailsAccount(OidcKeycloakAccount account, String username, String firstName, String familyName,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(username, firstName, familyName, authorities);

        this.account = account;
    }

    @Override
    public KeycloakSecurityContext getKeycloakSecurityContext() {
        return account.getKeycloakSecurityContext();
    }

    @Override
    public Principal getPrincipal() {
        return account.getPrincipal();
    }

    @Override
    public Set<String> getRoles() {
        return account.getRoles();
    }
}