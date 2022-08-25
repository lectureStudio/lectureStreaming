package org.lecturestudio.web.portal.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lecturestudio.web.portal.security.TokenAuthenticationProvider;
import org.lecturestudio.web.portal.security.TokenSecurityConfiguration;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml2.metadata.ContactPerson;
import org.opensaml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml2.metadata.EmailAddress;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.GivenName;
import org.opensaml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.impl.AttributeConsumingServiceBuilder;
import org.opensaml.saml2.metadata.impl.ContactPersonBuilder;
import org.opensaml.saml2.metadata.impl.EmailAddressBuilder;
import org.opensaml.saml2.metadata.impl.GivenNameBuilder;
import org.opensaml.saml2.metadata.impl.RequestedAttributeBuilder;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.*;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.*;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
public class WebSecurityConfig {

	public static final Map<String, String> SSO_REQUESTED_ATTRIBUTES = Map.of(
		"givenName", "urn:oid:2.5.4.42",
		"surname", "urn:oid:2.5.4.4",
		"cn", "urn:oid:2.5.4.3",
		"eduPersonAffiliation", "urn:oid:1.3.6.1.4.1.5923.1.1.1.1"
	);


	@Configuration
    @Order(1)
	public static class SamlConfigurerAdatper extends WebSecurityConfigurerAdapter {
		@Value("${saml.sp}")
		private String samlAudience;

		@Autowired
		@Qualifier("saml")
		private SavedRequestAwareAuthenticationSuccessHandler samlAuthSuccessHandler;

		@Autowired
		@Qualifier("saml")
		private SimpleUrlAuthenticationFailureHandler samlAuthFailureHandler;

		@Autowired
		private SAMLEntryPoint samlEntryPoint;

		@Autowired
		private SAMLLogoutFilter samlLogoutFilter;

		@Autowired
		private SAMLLogoutProcessingFilter samlLogoutProcessingFilter;

		@Autowired
		private SAMLAuthenticationProvider samlAuthenticationProvider;

		@Autowired
		private ExtendedMetadata extendedMetadata;

		@Autowired
		private KeyManager keyManager;


		@Bean
		public SAMLDiscovery samlDiscovery() {
			return new SAMLDiscovery();
		}

		@Bean
		public MetadataDisplayFilter metadataDisplayFilter() {
			return new MetadataDisplayFilter() {

				@Override
				protected void processMetadataDisplay(HttpServletRequest request, HttpServletResponse response)
						throws IOException, ServletException {
					try {
						SAMLMessageContext context = contextProvider.getLocalEntity(request, response);
						String entityId = context.getLocalEntityId();

						response.setContentType("text/xml");
						response.setCharacterEncoding("UTF-8");

						displayMetadata(entityId, response.getWriter());
					}
					catch (MetadataProviderException e) {
						throw new ServletException("Error initializing metadata", e);
					}
				}
			};
		}

		public MetadataGenerator metadataGenerator() {
			MetadataGenerator metadataGenerator = new MetadataGenerator() {

				@Override
				public EntityDescriptor generateMetadata() {
					final EntityDescriptor entityDescriptor = super.generateMetadata();
					final SPSSODescriptor spDescriptor = entityDescriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
					final AttributeConsumingService attributeService = new AttributeConsumingServiceBuilder().buildObject();

					for (var entry : SSO_REQUESTED_ATTRIBUTES.entrySet()) {
						final RequestedAttribute requestAttribute = new RequestedAttributeBuilder().buildObject();
						requestAttribute.setIsRequired(true);
						requestAttribute.setName(entry.getValue());
						requestAttribute.setFriendlyName(entry.getKey());
						requestAttribute.setNameFormat(Attribute.URI_REFERENCE);

						attributeService.getRequestAttributes().add(requestAttribute);
					}

					spDescriptor.getAttributeConsumingServices().add(attributeService);

					ContactPerson contactPerson = new ContactPersonBuilder().buildObject();
					contactPerson.setType(ContactPersonTypeEnumeration.TECHNICAL);

					GivenName givenName = new GivenNameBuilder().buildObject();
					givenName.setName("Alex Andres");

					EmailAddress emailAddress = new EmailAddressBuilder().buildObject();
					emailAddress.setAddress("alexej.andres@es.tu-darmstadt.de");

					contactPerson.setGivenName(givenName);
					contactPerson.getEmailAddresses().add(emailAddress);

					entityDescriptor.getContactPersons().add(contactPerson);

					return entityDescriptor;
				}
			};
			metadataGenerator.setEntityId(samlAudience);
			metadataGenerator.setEntityBaseURL(samlAudience);
			metadataGenerator.setExtendedMetadata(extendedMetadata);
			metadataGenerator.setIncludeDiscoveryExtension(false);
			metadataGenerator.setKeyManager(keyManager);

			return metadataGenerator;
		}

		@Bean
		public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
			SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
			samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
			samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(samlAuthSuccessHandler);
			samlWebSSOProcessingFilter.setAuthenticationFailureHandler(samlAuthFailureHandler);
	
			return samlWebSSOProcessingFilter;
		}

		@Bean
		public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
			SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter = new SAMLWebSSOHoKProcessingFilter();
			samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(samlAuthSuccessHandler);
			samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
			samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(samlAuthFailureHandler);
			return samlWebSSOHoKProcessingFilter;
		}

		@Bean
		public FilterChainProxy samlFilter() throws Exception {
			List<SecurityFilterChain> chains = new ArrayList<>();
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"), samlEntryPoint));
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"), samlLogoutFilter));
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"), metadataDisplayFilter()));
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"), samlWebSSOProcessingFilter()));
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"), samlWebSSOHoKProcessingFilter()));
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/discovery/**"), samlDiscovery()));
			chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"), samlLogoutProcessingFilter));

			return new FilterChainProxy(chains);
		}

		@Bean
		@Override
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		@Bean
		public MetadataGeneratorFilter metadataGeneratorFilter() {
			return new MetadataGeneratorFilter(metadataGenerator());
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.csrf()
				.disable();

			http
				.httpBasic()
					.authenticationEntryPoint(samlEntryPoint);

			http
				.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
				.addFilterAfter(samlFilter(), BasicAuthenticationFilter.class)
				.addFilterBefore(samlFilter(), CsrfFilter.class);

			http
				.authorizeRequests()
					.antMatchers("/").permitAll()
					.antMatchers("/p2p-demo").permitAll()
					.antMatchers("/contact").permitAll()
					.antMatchers("/sponsors").permitAll()
					.antMatchers("/imprint").permitAll()
					.antMatchers("/privacy").permitAll()
					.antMatchers("/janus/**").permitAll()
					.antMatchers("/css/**").permitAll()
					.antMatchers("/images/**").permitAll()
					.antMatchers("/js/**").permitAll()
					.antMatchers("/manual/**").permitAll()
					.antMatchers("/api/publisher/**").permitAll()	// Will be handled by the personal token authentification.
					.antMatchers("/messenger/**").permitAll()
					.antMatchers("/app/**").permitAll()
					.antMatchers("/message/**").permitAll()
					.antMatchers("/saml/**").permitAll()
					.antMatchers("/course/{courseId:[\\d+]}").access("@webSecurity.checkCourseId(authentication,#courseId)")
					.antMatchers("/course/{courseId:[\\d+]}/**").access("@webSecurity.checkCourseId(authentication,#courseId)")
					.anyRequest().authenticated();

			http
				.logout().disable();
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(samlAuthenticationProvider);
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
