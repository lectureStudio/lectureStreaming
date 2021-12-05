package org.lecturestudio.web.portal.controller;

import static java.util.Objects.nonNull;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultErrorController implements ErrorController {

	@RequestMapping("/error")
	public String handleError(Model model, HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

		if (nonNull(status)) {
			Integer statusCode = Integer.valueOf(status.toString());
			HttpStatus httpStatus;

			try {
				httpStatus = HttpStatus.valueOf(statusCode);
			}
			catch (IllegalArgumentException e) {
				return "error";
			}

			model.addAttribute("code", statusCode);
			model.addAttribute("phrase", httpStatus.getReasonPhrase());

			if (httpStatus.is4xxClientError()) {
				return "error/client";
			}
			else if (httpStatus.is5xxServerError()) {
				return "error/server";
			}
		}

		return "error";
	}
}
