package org.lecturestudio.web.portal.validator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lecturestudio.web.api.model.ClassroomServiceResponse;
import org.lecturestudio.web.api.model.ClassroomServiceResponse.Status;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;

@Component
public class SpeechValidator {

	private final Map<Long, Long> historyMap = new ConcurrentHashMap<>();


	public ResponseEntity<ClassroomServiceResponse> validate(long courseId) {
		BodyBuilder responseBuilder;

		if (historyMap.containsKey(courseId)) {
			ClassroomServiceResponse serviceResponse = new ClassroomServiceResponse();
			serviceResponse.statusCode = Status.ERROR.getCode();
			serviceResponse.statusMessage = "speech.attempts.max";

			responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
			responseBuilder.body(serviceResponse);
		}
		else {
			ClassroomServiceResponse serviceResponse = new ClassroomServiceResponse();
			serviceResponse.statusCode = Status.SUCCESS.getCode();
			serviceResponse.statusMessage = "speech.request.sent";

			responseBuilder = ResponseEntity.ok();
			responseBuilder.body(serviceResponse);
		}

		return responseBuilder.build();
	}

	public void registerRequest(long courseId) {
		historyMap.put(courseId, System.currentTimeMillis());
	}

	public void unregisterRequest(long courseId) {
		historyMap.remove(courseId);
	}
}
