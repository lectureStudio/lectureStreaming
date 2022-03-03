package org.lecturestudio.web.portal.keycloak;

import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.service.RoleService;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class KeycloakUserDetailsAuthenticationProvider extends KeycloakAuthenticationProvider {

    private final UserService userService;

    private final RoleService roleService;


    public KeycloakUserDetailsAuthenticationProvider(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
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

        Set<String> userRoleStrings = account.getRoles();
        Set<Role> userRoles = userRoleStrings.stream().filter((s) -> {
            return roleService.existsByName(s);
        }).map((s) -> {
            return roleService.findRoleByName(s).get();
        }).collect(Collectors.toSet());

        if (userOpt.isEmpty()) {
            UUID uuid = null;
            do {
                uuid = UUID.randomUUID();
            } while (userService.hasUser(uuid));

            userService.saveUser(User.builder()
                    .userId(username)
                    .anonymousUserId(uuid)
                    .firstName(firstName)
                    .familyName(familyName)
                    .roles(userRoles)
                    .build());
        }

        KeycloakUserDetailsAccount detailsAccount = new KeycloakUserDetailsAccount(account, username,
                firstName, familyName, token.getAuthorities());

        return new KeycloakAuthenticationToken(detailsAccount, token.isInteractive(), token.getAuthorities());
    }

}
