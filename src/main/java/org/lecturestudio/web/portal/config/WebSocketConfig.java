package org.lecturestudio.web.portal.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.websocket.CourseFeatureWebSocketHandler;
import org.lecturestudio.web.portal.websocket.CourseStateWebSocketHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private CourseFeatureState courseFeatureState;

	@Autowired
	private CourseMessengerFeatureSaveFeature messengerSaveFeature;

  @Autowired
	private UserService userService;


	@Autowired
	private ObjectMapper objectMapper;


	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry
			.addHandler(new CourseStateWebSocketHandler(courseStates, objectMapper, userService, messengerSaveFeature), "/api/publisher/course-state")
				.setAllowedOrigins("*")
			.addHandler(new CourseFeatureWebSocketHandler(courseFeatureState, objectMapper, messengerSaveFeature), "/api/publisher/messages")
				.setAllowedOrigins("*");
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(8192);
		container.setMaxBinaryMessageBufferSize(8192);

		return container;
	}

	@Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic").setTaskScheduler(heartBeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/publisher/messenger").setAllowedOriginPatterns("*").withSockJS();
		registry.addEndpoint("/api/subscriber/messenger").setAllowedOriginPatterns("*").withSockJS();
    }

	@Bean
    public TaskScheduler heartBeatScheduler() {
        return new ThreadPoolTaskScheduler();
    }
}
