package org.lecturestudio.web.portal.service;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseSpeechRequest;
import org.lecturestudio.web.portal.repository.CourseSpeechRequestRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseSpeechRequestService {

	@Autowired
	private CourseSpeechRequestRepository requestRepository;


	public void deleteById(Long id) {
		requestRepository.deleteById(id);
	}

	public void saveRequest(CourseSpeechRequest request) {
		requestRepository.save(request);
	}

	public Optional<CourseSpeechRequest> findByRequestId(long requestId) {
		return requestRepository.findByRequestId(requestId);
	}
}
