package org.lecturestudio.web.portal.service;

import java.util.Optional;

import javax.transaction.Transactional;

import org.lecturestudio.web.portal.model.CourseParticipant;
import org.lecturestudio.web.portal.repository.CourseParticipantRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseParticipantService {

	@Autowired
	private CourseParticipantRepository featureRepository;


	public void saveParticipant(CourseParticipant participant) {
		System.out.println("+: " + participant.getSessionId() + " " + participant.getUserId());

		featureRepository.deleteAll();

		featureRepository.save(participant);
	}

	@Transactional
	public void deleteParticipantBySessionId(String sessionId) {
		System.out.println("-: " + sessionId);

		featureRepository.deleteBySessionId(sessionId);
	}

	public Optional<CourseParticipant> getParticipantBySessionId(String sessionId) {
		System.out.println("#: " + sessionId + " " + featureRepository.findBySessionId(sessionId));

		return featureRepository.findBySessionId(sessionId);
	}
}
