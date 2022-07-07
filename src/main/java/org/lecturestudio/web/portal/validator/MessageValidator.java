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
		CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
		BodyBuilder responseBuilder;

		if (isNull(feature)) {
			serviceResponse.statusCode = 1;
			serviceResponse.statusMessage = "message.service.absent";

			responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
		}
		else {
			Map<Integer, String> fieldErrors = new HashMap<>();

			try {
				validateServiceId(feature, message);
				validateInputFields(message, fieldErrors);

				serviceResponse.statusCode = 0;
				serviceResponse.statusMessage = "course.feature.message.sent";

				responseBuilder = ResponseEntity.ok();
			}
			catch (Exception e) {
				serviceResponse.statusCode = 2;
				serviceResponse.statusMessage = "course.feature.message.send.error";

				responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
			}
		}

		return responseBuilder.body(serviceResponse);
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
