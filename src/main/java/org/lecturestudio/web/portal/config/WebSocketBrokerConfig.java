package org.lecturestudio.web.portal.config;

import org.lecturestudio.web.portal.interceptor.StompHandshakeInterceptor;
import org.lecturestudio.web.portal.interceptor.StompInboundInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/queue", "/topic");
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/api/publisher/messages").addInterceptors(stompHandshakeInterceptor());
		registry.addEndpoint("/ws-state").addInterceptors(stompHandshakeInterceptor());
	}

	@Bean
	public StompHandshakeInterceptor stompHandshakeInterceptor() {
		return new StompHandshakeInterceptor();
	}

	@Bean
	public StompInboundInterceptor stompInboundInterceptor() {
		return new StompInboundInterceptor();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompInboundInterceptor());
	}
}
