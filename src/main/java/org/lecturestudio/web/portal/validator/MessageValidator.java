package org.lecturestudio.web.portal.validator;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;

import org.lecturestudio.web.api.model.Message;
import org.lecturestudio.web.api.stream.model.CourseFeatureResponse;
import org.lecturestudio.web.portal.model.CourseMessageFeature;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {
	
	public ResponseEntity<CourseFeatureResponse> validate(CourseMessageFeature feature, Message message) {
		BodyBuilder responseBuilder;

		if (isNull(feature)) {
			CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
			serviceResponse.statusCode = 1;
			serviceResponse.statusMessage = "message.service.absent";

			responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
			responseBuilder.body(serviceResponse);
		}
		else {
			Map<Integer, String> fieldErrors = new HashMap<>();

			try {
				validateServiceId(feature, message);
				validateInputFields(message, fieldErrors);

				CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
				serviceResponse.statusCode = 0;
				serviceResponse.statusMessage = "message.sent";

				responseBuilder = ResponseEntity.ok();
				responseBuilder.body(serviceResponse);
			}
			catch (Exception e) {
				CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
				serviceResponse.statusCode = 2;
				serviceResponse.statusMessage = e.getMessage();
				// serviceResponse.data = fieldErrors;

				responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
				responseBuilder.body(serviceResponse);
			}
		}

		return responseBuilder.build();
	}

	private static void validateServiceId(CourseMessageFeature feature, Message message) throws Exception {
		if (!feature.getFeatureId().equals(message.getServiceId())) {
			throw new Exception("message.service.absent");
		}
	}

	private static void validateInputFields(Message message, Map<Integer, String> fieldErrors) throws Exception {
		String messageStr = message.getText();

		if (isNull(messageStr) || messageStr.isEmpty()) {
			fieldErrors.put(0, "message.input.empty");
		}

		if (!fieldErrors.isEmpty()) {
			throw new Exception("message.input.invalid");
		}
	}

}
