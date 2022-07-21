package org.lecturestudio.web.portal.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import org.lecturestudio.web.portal.saml.LectSAMLUserDetailsService;
import org.lecturestudio.web.portal.service.RoleService;
import org.lecturestudio.web.portal.service.UserService;

import org.opensaml.saml2.metadata.provider.AbstractReloadingMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.parse.StaticBasicParserPool;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.saml.*;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.processor.*;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.*;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

@Configuration
public class SamlSecurityConfig implements InitializingBean, DisposableBean {

	@Value("${saml.keystore.location}")
	private String samlKeystoreLocation;

	@Value("${saml.keystore.password}")
	private String samlKeystorePassword;

	@Value("${saml.keystore.alias}")
	private String samlKeystoreAlias;

	@Value("${saml.idp}")
	private String defaultIdp;

	@Value("#{'${saml.idp.key.aliases}'.split(',')}")
	private Set<String> idpKeyAliases;

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

	private Timer backgroundTaskTimer;

	private MultiThreadedHttpConnectionManager httpConnectionManager;


	@Override
	public void destroy() throws Exception {
		backgroundTaskTimer.purge();
		backgroundTaskTimer.cancel();

		httpConnectionManager.shutdown();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		backgroundTaskTimer = new Timer(true);
		httpConnectionManager = new MultiThreadedHttpConnectionManager();
	}

	@Bean
    public HttpClient httpClient() {
        return new HttpClient(httpConnectionManager);
    }

	@Bean(initMethod = "initialize")
	public StaticBasicParserPool parserPool() {
		return new StaticBasicParserPool();
	}

	@Bean
	public SAMLAuthenticationProvider samlAuthenticationProvider() {
		SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
        // samlAuthenticationProvider.setUserDetails(samlUserDetailsServiceImpl);
        samlAuthenticationProvider.setForcePrincipalAsString(false);
        return samlAuthenticationProvider;
		// return new SAMLAuthenticationProvider();
	}

	@Bean
	public SAMLUserDetailsService samlUserDetailsService() {
		return new LectSAMLUserDetailsService(userService, roleService);
	}

	@Bean
	public SAMLContextProviderImpl contextProvider() {
		return new SAMLContextProviderImpl();
	}

	@Bean
	public static SAMLBootstrap samlBootstrap() {
		return new SAMLBootstrap();
	}

	@Bean
	public SAMLDefaultLogger samlLogger() {
		return new SAMLDefaultLogger();
	}

	@Bean
	public WebSSOProfileConsumer webSSOprofileConsumer() {
		return new WebSSOProfileConsumerImpl();
	}

	@Bean
	@Qualifier("hokWebSSOprofileConsumer")
	public WebSSOProfileConsumerHoKImpl hokWebSSOProfileConsumer() {
		return new WebSSOProfileConsumerHoKImpl();
	}

	@Bean
	public WebSSOProfile webSSOprofile() {
		return new WebSSOProfileImpl();
	}

	@Bean
	public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
		return new WebSSOProfileConsumerHoKImpl();
	}

	@Bean
	public WebSSOProfileECPImpl ecpProfile() {
		return new WebSSOProfileECPImpl();
	}

	@Bean
	public SingleLogoutProfile logoutProfile() {
		return new SingleLogoutProfileImpl();
	}

	@Bean
	public KeyManager keyManager() {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		Resource storeFile = loader.getResource(samlKeystoreLocation);
		Map<String, String> passwords = new HashMap<>();
		passwords.put(samlKeystoreAlias, samlKeystorePassword);

		return new JKSKeyManager(storeFile, samlKeystorePassword, passwords, samlKeystoreAlias);
	}

	@Bean
	public WebSSOProfileOptions defaultWebSSOProfileOptions() {
		WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
		webSSOProfileOptions.setIncludeScoping(false);

		return webSSOProfileOptions;
	}

	@Bean
	public SAMLEntryPoint samlEntryPoint() {
		SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
		samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());

		return samlEntryPoint;
	}

	@Bean
	public ExtendedMetadata extendedMetadata() {
		ExtendedMetadata extendedMetadata = new ExtendedMetadata();
		extendedMetadata.setIdpDiscoveryEnabled(false);
		extendedMetadata.setSignMetadata(false);

		return extendedMetadata;
	}

	@Bean
	public ExtendedMetadataDelegate extendedMetadataProvider() throws MetadataProviderException {
		AbstractReloadingMetadataProvider provider = new HTTPMetadataProvider(backgroundTaskTimer, httpClient(), defaultIdp);
		provider.setParserPool(parserPool());

		ExtendedMetadataDelegate delegate = new ExtendedMetadataDelegate(provider, extendedMetadata());
		delegate.setMetadataTrustedKeys(idpKeyAliases);
		delegate.setMetadataTrustCheck(true);
		//delegate.setMetadataRequireSignature(true);

		return delegate;
	}

	@Bean
	@Qualifier("metadata")
	public CachingMetadataManager metadata() throws MetadataProviderException, ResourceException {
		List<MetadataProvider> providers = new ArrayList<>();
		providers.add(extendedMetadataProvider());

		CachingMetadataManager metadataManager = new CachingMetadataManager(providers);
		metadataManager.setDefaultIDP(defaultIdp);

		return metadataManager;
	}

	@Bean
	@Qualifier("saml")
	public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
		SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SavedRequestAwareAuthenticationSuccessHandler();
		successRedirectHandler.setDefaultTargetUrl("/home");

		return successRedirectHandler;
	}

	@Bean
	@Qualifier("saml")
	public SimpleUrlAuthenticationFailureHandler samlAuthenticationFailureHandler() {
		SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
		failureHandler.setUseForward(true);
		failureHandler.setDefaultFailureUrl("/error");

		return failureHandler;
	}

	@Bean
	public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
		SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
		successLogoutHandler.setDefaultTargetUrl("/");

		return successLogoutHandler;
	}

	@Bean
	public SecurityContextLogoutHandler logoutHandler() {
		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.setInvalidateHttpSession(true);
		logoutHandler.setClearAuthentication(true);

		return logoutHandler;
	}

	@Bean
	public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
		return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
	}

	@Bean
	public SAMLLogoutFilter samlLogoutFilter() {
		return new SAMLLogoutFilter(successLogoutHandler(),
			new LogoutHandler[] { logoutHandler() },
			new LogoutHandler[] { logoutHandler() }
		);
	}

	@Bean
	public HTTPPostBinding httpPostBinding() {
		return new HTTPPostBinding(parserPool(), VelocityFactory.getEngine());
	}

	@Bean
	public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
		return new HTTPRedirectDeflateBinding(parserPool());
	}

	@Bean
	public SAMLProcessorImpl processor() {
		ArrayList<SAMLBinding> bindings = new ArrayList<>();
		bindings.add(httpRedirectDeflateBinding());
		bindings.add(httpPostBinding());

		return new SAMLProcessorImpl(bindings);
	}
}
