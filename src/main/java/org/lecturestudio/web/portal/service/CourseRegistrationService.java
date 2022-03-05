package org.lecturestudio.web.portal.service;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseRegistration;
import org.lecturestudio.web.portal.repository.CourseRegistrationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseRegistrationService {

	@Autowired
	private CourseRegistrationRepository registrationRepository;


	public Optional<CourseRegistration> findByCourseAndUserId(Long courseId, String userId) {
		return registrationRepository.findByCourseAndUserId(courseId, userId);
	}

	public Optional<CourseRegistration> findByCourse(Long courseId) {
		return registrationRepository.findByCourse(courseId);
	}
}
