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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("status", "34");
		responseMap.put("message", "unauthorized access");

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		OutputStream out = response.getOutputStream();
		ObjectMapper mapper = new ObjectMapper();

		mapper.writerWithDefaultPrettyPrinter().writeValue(out, responseMap);
		out.flush();
	}
	
}
