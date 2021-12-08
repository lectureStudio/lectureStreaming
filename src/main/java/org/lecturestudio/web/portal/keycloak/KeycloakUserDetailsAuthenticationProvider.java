package org.lecturestudio.web.portal.keycloak;

import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;

import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

public class KeycloakUserDetailsAuthenticationProvider extends KeycloakAuthenticationProvider {

    private final UserService userService;


    public KeycloakUserDetailsAuthenticationProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) super.authenticate(authentication);

        if (token == null) {
            return null;
        }

        OidcKeycloakAccount account = token.getAccount();
        AccessToken accessToken = account.getKeycloakSecurityContext().getToken();

        String username = accessToken.getPreferredUsername();
        String firstName = accessToken.getGivenName();
        String familyName = accessToken.getFamilyName();

        Optional<User> userOpt = userService.findById(username);

        if (userOpt.isEmpty()) {
            userService.saveUser(User.builder()
                    .userId(username)
                    .firstName(firstName)
                    .familyName(familyName)
                    .build());
        }

        KeycloakUserDetailsAccount detailsAccount = new KeycloakUserDetailsAccount(account, username,
                firstName, familyName, token.getAuthorities());

        return new KeycloakAuthenticationToken(detailsAccount, token.isInteractive(), token.getAuthorities());
    }

}
