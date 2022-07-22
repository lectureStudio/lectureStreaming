package org.lecturestudio.web.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 	registry.enableSimpleBroker("/queue", "/topic").setTaskScheduler(heartBeatScheduler());
		config.enableSimpleBroker("/queue", "/topic");
		config.setApplicationDestinationPrefixes("/app");
		// 	registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/api/publisher/messages").setAllowedOriginPatterns("*");
		registry.addEndpoint("/api/subscriber/messenger").setAllowedOriginPatterns("*");

		registry.addEndpoint("/ws-state");
	}

	// @Bean
	// public StompInboundChannelInterceptor stompChannelInterceptor() {
	// 	return new StompInboundChannelInterceptor();
	// }

	// @Override
	// public void configureClientInboundChannel(ChannelRegistration registration) {
	// 	registration.interceptors(stompChannelInterceptor());
	// }

	// @Bean
	// public TaskScheduler heartBeatScheduler() {
	// 	return new ThreadPoolTaskScheduler();
	// }
}
