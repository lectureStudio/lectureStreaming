package org.lecturestudio.web.portal.service;

import java.util.Optional;

import javax.transaction.Transactional;

import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.repository.CourseParticipantRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CourseParticipantService {

	@Autowired
	private CourseParticipantRepository featureRepository;


	public void saveParticipant(CourseParticipant participant) {
		featureRepository.save(participant);
	}

	@Transactional
	public void deleteParticipantBySessionId(String sessionId) {
		featureRepository.deleteBySessionId(sessionId);
	}

	public Iterable<User> findAllUsersByCourseId(Long courseId) {
		return featureRepository.findAllUsersByCourseId(courseId, Sort.by(Sort.Direction.ASC, "user.familyName"));
	}

	public Optional<CourseParticipant> getParticipantBySessionId(String sessionId) {
		return featureRepository.findBySessionId(sessionId);
	}

	public boolean existsByUserId(String userId) {
		return featureRepository.existsByUser(userId);
	}
}
