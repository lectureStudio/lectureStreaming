package org.lecturestudio.web.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

// @Configuration
public class WebSocketBrokerSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

	@Override
	protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
		messages
				.nullDestMatcher().authenticated()
				// .simpMessageDestMatchers("**").denyAll()
				.simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").permitAll()
				.simpDestMatchers("/app/**").permitAll()
				.anyMessage().denyAll();
	}

	@Override
	protected boolean sameOriginDisabled() {
		// While CSRF is disabled..
		return true;
	}
}
