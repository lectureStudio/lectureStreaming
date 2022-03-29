package org.lecturestudio.web.portal.validator;

import static java.util.Objects.isNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.lecturestudio.web.api.filter.FilterRule;
import org.lecturestudio.web.api.filter.InputFieldRule;
import org.lecturestudio.web.api.model.quiz.Quiz;
import org.lecturestudio.web.api.model.quiz.QuizAnswer;
import org.lecturestudio.web.api.stream.model.CourseFeatureResponse;
import org.lecturestudio.web.portal.model.CourseQuizFeature;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;

@Component
public class QuizAnswerValidator {

	public ResponseEntity<CourseFeatureResponse> validate(String userName, CourseQuizFeature feature, QuizAnswer quizAnswer) {
		CourseFeatureResponse serviceResponse = new CourseFeatureResponse();
		BodyBuilder responseBuilder;

		if (isNull(feature)) {
			serviceResponse.statusCode = 1;
			serviceResponse.statusMessage = "quiz.service.absent";

			responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
		}
		else {
			Map<Integer, String> fieldErrors = new HashMap<>();

			try {
				validateUser(feature, userName);
				validateServiceId(feature, quizAnswer.getServiceId());
				validateInputFields(feature, quizAnswer.getOptions(), fieldErrors);

				serviceResponse.statusCode = 0;
				serviceResponse.statusMessage = "course.feature.quiz.sent";

				responseBuilder = ResponseEntity.ok();
			}
			catch (Exception e) {
				serviceResponse.statusCode = 2;
				serviceResponse.statusMessage = e.getMessage();
				// serviceResponse.data = fieldErrors;

				responseBuilder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
			}
		}

		return responseBuilder.body(serviceResponse);
	}

	private static void validateUser(CourseQuizFeature feature, String userName) throws Exception {
		if (feature.getUsers().contains(userName)) {
			throw new Exception("course.feature.quiz.count.error");
		}
	}

	private static void validateServiceId(CourseQuizFeature feature, String serviceId) throws Exception {
		String quizServiceId = feature.getFeatureId();

		if (!quizServiceId.equals(serviceId)) {
			throw new Exception("course.feature.absent");
		}
	}

	private static void validateInputFields(CourseQuizFeature feature, String[] options, Map<Integer, String> fieldErrors) throws Exception {
		if (isNull(options)) {
			options = new String[0];
		}

		if (options.length > feature.getOptions().size()) {
			throw new Exception("course.feature.quiz.input.invalid");
		}

		// Check for possible blacklisted input.
		if (feature.getType() == Quiz.QuizType.NUMERIC) {
			// int fieldId = 0;

			// for (String optionValue : options) {
			// 	for (FilterRule<String> r : feature.getInputFilter().getRules()) {
			// 		InputFieldRule<String> rule = (InputFieldRule<String>) r;
			// 		if (!rule.isAllowed(optionValue, fieldId)) {
			// 			String error = "quiz.answer.input.error";

			// 			fieldErrors.put(fieldId, error);
			// 		}
			// 	}
			// 	fieldId++;
			// }
		}

		if (!fieldErrors.isEmpty()) {
			throw new Exception("course.feature.quiz.input.invalid");
		}
	}

}
