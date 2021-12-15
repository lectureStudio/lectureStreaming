package org.lecturestudio.web.portal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

import org.lecturestudio.web.portal.model.CourseConnectionRequest;
import org.lecturestudio.web.portal.repository.CourseConnectionRequestRepository;

@Service
public class CourseConnectionRequestService {

	@Autowired
	private CourseConnectionRequestRepository requestRepository;


	public void deleteById(Long id) {
		requestRepository.deleteById(id);
	}

	public void saveRequest(CourseConnectionRequest request) {
		requestRepository.save(request);
	}

	public Optional<CourseConnectionRequest> findByRequestId(long requestId) {
		return requestRepository.findByRequestId(requestId);
	}

	public Iterable<CourseConnectionRequest> getAllEntries() {
		return requestRepository.findAll();
	}

	public void deleteAll() {
		this.requestRepository.deleteAll();
	}

	public List<CourseConnectionRequest> getAllByCourseId(long courseId) {
		return this.requestRepository.findUsersByCourseId(courseId);
	}

	public Long findNumRequestsToCourseOfUser(String userId, Long courseId) {
		return this.requestRepository.findNumRequestsToCourseOfUser(userId, courseId);
	}
}