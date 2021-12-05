package org.lecturestudio.web.portal.service;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseFeature;
import org.lecturestudio.web.portal.model.CourseMessageFeature;
import org.lecturestudio.web.portal.model.CourseQuizFeature;
import org.lecturestudio.web.portal.repository.CourseFeatureRepository;
import org.lecturestudio.web.portal.repository.MessageFeatureRepository;
import org.lecturestudio.web.portal.repository.QuizFeatureRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseFeatureService {

	@Autowired
	private CourseFeatureRepository featureRepository;

	@Autowired
	private MessageFeatureRepository messageRepository;

	@Autowired
	private QuizFeatureRepository quizRepository;


	public void deleteById(Long id) {
		featureRepository.deleteById(id);
	}

	public void save(CourseFeature feature) {
		featureRepository.save(feature);
	}

	public Optional<CourseMessageFeature> findMessageByCourseId(long courseId) {
		return messageRepository.findByCourseId(courseId);
	}

	public Optional<CourseQuizFeature> findQuizByCourseId(long courseId) {
		return quizRepository.findByCourseId(courseId);
	}
}
