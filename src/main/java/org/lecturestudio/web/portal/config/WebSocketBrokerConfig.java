package org.lecturestudio.web.portal.config;

import org.lecturestudio.web.portal.interceptor.StompHandshakeInterceptor;
import org.lecturestudio.web.portal.interceptor.StompInboundInterceptor;
import org.lecturestudio.web.portal.property.SimpProperties;

import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private SimpProperties simpProperties;


	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker(simpProperties.getPrefixes().getBroker());
		config.setApplicationDestinationPrefixes(simpProperties.getPrefixes().getApp());
		config.setUserDestinationPrefix(simpProperties.getPrefixes().getUser());
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(simpProperties.getEndpoints().getPublisher()).addInterceptors(stompHandshakeInterceptor());
		registry.addEndpoint(simpProperties.getEndpoints().getState()).addInterceptors(stompHandshakeInterceptor());
		registry.addEndpoint("/p2p").addInterceptors(stompHandshakeInterceptor());
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
