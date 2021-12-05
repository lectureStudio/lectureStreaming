package org.lecturestudio.web.portal.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("status", "403");
		responseMap.put("message", "unauthorized access");

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);

		OutputStream out = response.getOutputStream();
		ObjectMapper mapper = new ObjectMapper();

		mapper.writerWithDefaultPrettyPrinter().writeValue(out, responseMap);
		out.flush();
	}
	
}
