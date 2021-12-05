package org.lecturestudio.web.portal.validator;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;

import org.lecturestudio.web.api.model.ClassroomServiceResponse;
import org.lecturestudio.web.api.model.Message;
import org.lecturestudio.web.api.model.ClassroomServiceResponse.Status;
import org.lecturestudio.web.portal.model.CourseMessageFeature;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {
	
	public ResponseEntity<ClassroomServiceResponse> validate(CourseMessageFeature feature, Message message) {
		BodyBuilder responseBuilder;

		if (isNull(feature)) {
			ClassroomServiceResponse serviceResponse = new ClassroomServiceResponse();
			serviceResponse.statusCode = Status.ERROR.getCode();
			serviceResponse.statusMessage = "message.service.absent";

			responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
			responseBuilder.body(serviceResponse);
		}
		else {
			Map<Integer, String> fieldErrors = new HashMap<>();

			try {
				validateServiceId(feature, message);
				validateInputFields(message, fieldErrors);

				ClassroomServiceResponse serviceResponse = new ClassroomServiceResponse();
				serviceResponse.statusCode = Status.SUCCESS.getCode();
				serviceResponse.statusMessage = "message.sent";

				responseBuilder = ResponseEntity.ok();
				responseBuilder.body(serviceResponse);
			}
			catch (Exception e) {
				ClassroomServiceResponse serviceResponse = new ClassroomServiceResponse();
				serviceResponse.statusCode = Status.DATA_ERROR.getCode();
				serviceResponse.statusMessage = e.getMessage();
				serviceResponse.data = fieldErrors;

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
