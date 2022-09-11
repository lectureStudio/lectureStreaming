package org.lecturestudio.web.portal.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.service.UserService;
import org.lecturestudio.web.portal.websocket.CourseFeatureWebSocketHandler;
import org.lecturestudio.web.portal.websocket.CourseStateWebSocketHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private CourseStates courseStates;

	@Autowired
	private CourseFeatureState courseFeatureState;

	@Autowired
	private UserService userService;

	@Autowired
	private ObjectMapper objectMapper;


	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry
			.addHandler(new CourseStateWebSocketHandler(courseStates, objectMapper, userService), "/api/everyonelmao/course-state")
				.setAllowedOrigins("*");
		registry
			.addHandler(new CourseFeatureWebSocketHandler(courseFeatureState, objectMapper), "/api/publisher/messages")
				.setAllowedOrigins("*");
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(8192);
		container.setMaxBinaryMessageBufferSize(8192);

		return container;
	}
}
