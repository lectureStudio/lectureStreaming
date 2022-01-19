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

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

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
					.antMatchers("/api/publisher/**").permitAll()	// Will be handled by the personal token authentication.
					.antMatchers("/messenger/**").permitAll()
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
