package org.lecturestudio.web.portal.config;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;

import org.lecturestudio.web.portal.keycloak.KeycloakUserDetailsAuthenticationProvider;
import org.lecturestudio.web.portal.security.TokenAuthenticationProvider;
import org.lecturestudio.web.portal.security.TokenSecurityConfiguration;
import org.lecturestudio.web.portal.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.Map;

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

	public static final Map<String, String> SSO_REQUESTED_ATTRIBUTES = Map.of(
			"givenName", "urn:oid:2.5.4.42",
			"surname", "urn:oid:2.5.4.4",
			"cn", "urn:oid:2.5.4.3",
			"eduPersonAffiliation", "urn:oid:1.3.6.1.4.1.5923.1.1.1.1"
	);

	@Configuration
	@Order(1)
	@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
	public static class KeycloakSecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

		@Autowired
		private UserService userService;


		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
			keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());

			auth.authenticationProvider(keycloakAuthenticationProvider);
		}

		@Override
		protected KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
			return new KeycloakUserDetailsAuthenticationProvider(userService);
		}

		@Bean
		public KeycloakSpringBootConfigResolver KeycloakConfigResolver() {
			return new KeycloakSpringBootConfigResolver();
		}

		@Bean
		@Override
		protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
			return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			super.configure(http);

			http
					.csrf()
					.disable();

			http
					.authorizeRequests()
					.antMatchers("/").permitAll()
					.antMatchers("/contact").permitAll()
					.antMatchers("/imprint").permitAll()
					.antMatchers("/privacy").permitAll()
					.antMatchers("/janus/**").permitAll()
					.antMatchers("/css/**").permitAll()
					.antMatchers("/images/**").permitAll()
					.antMatchers("/js/**").permitAll()
					.antMatchers("/manual/**").permitAll()
					.antMatchers("/api/publisher/**").permitAll()	// Will be handled by the personal token authentification.
					.anyRequest().authenticated();
		}

	}

	@Configuration
	@Order(2)
	public static class TokenConfigurerAdatper extends TokenSecurityConfiguration {

		public TokenConfigurerAdatper(TokenAuthenticationProvider authenticationProvider) {
			super(authenticationProvider);
		}

	}
}
