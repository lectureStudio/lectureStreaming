package org.lecturestudio.web.portal.interceptor;

import java.util.Map;

import javax.transaction.Transactional;

import org.lecturestudio.web.portal.service.P2PDemoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class P2PDemoListener {

	@Autowired
	private P2PDemoService p2pDemoService;

	@Value("${simp.session.header.endpoint}")
	private String endpointHeader;


	@EventListener
	private void handleSessionConnected(SessionConnectEvent event) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionHeaders = headers.getSessionAttributes();
		String stompEndpoint = (String) sessionHeaders.get(endpointHeader);
		String userName = headers.getUser().getName();

		if ("/p2p".equals(stompEndpoint)) {
			p2pDemoService.registerUser(userName);
		}
	}

	@EventListener
	@Transactional
	private void handleSessionDisconnect(SessionDisconnectEvent event) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionHeaders = headers.getSessionAttributes();
		String stompEndpoint = (String) sessionHeaders.get(endpointHeader);
		String userName = headers.getUser().getName();

		if ("/p2p".equals(stompEndpoint)) {
			p2pDemoService.unregisterUser(userName);
		}
	}
}
