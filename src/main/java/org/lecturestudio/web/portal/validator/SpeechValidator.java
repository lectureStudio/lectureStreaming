package org.lecturestudio.web.portal.validator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lecturestudio.web.api.stream.model.CourseFeatureResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;

@Component
public class SpeechValidator {

	private final Map<Long, Long> historyMap = new ConcurrentHashMap<>();


	public ResponseEntity<CourseFeatureResponse> validate(long courseId) {
		BodyBuilder responseBuilder;

		if (historyMap.containsKey(courseId)) {
			CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
			serviceResponse.statusCode = 1;
			serviceResponse.statusMessage = "speech.attempts.max";

			responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
			responseBuilder.body(serviceResponse);
		}
		else {
			CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
			serviceResponse.statusCode = 0;
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
