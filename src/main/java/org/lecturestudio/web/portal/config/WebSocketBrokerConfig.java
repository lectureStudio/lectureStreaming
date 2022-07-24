package org.lecturestudio.web.portal.config;

import org.lecturestudio.web.portal.interceptor.StompHandshakeInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
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
		registry.addEndpoint("/api/subscriber/messages").addInterceptors(stompHandshakeInterceptor());
		registry.addEndpoint("/ws-state").addInterceptors(stompHandshakeInterceptor());
	}

	@Bean
	public StompHandshakeInterceptor stompHandshakeInterceptor() {
		return new StompHandshakeInterceptor();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				StompCommand messageCommand = accessor.getCommand();

				if (StompCommand.CONNECT.equals(messageCommand)) {
					String userName = accessor.getUser().getName();

					// System.out.println("Connect: " + userName + " "  + accessor.getSessionAttributes());
				}

				return message;
			}

		});
	}
}
