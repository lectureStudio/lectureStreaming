package org.lecturestudio.web.portal.service;

import java.util.Optional;

import org.lecturestudio.web.portal.model.CourseQuizResource;
import org.lecturestudio.web.portal.repository.CourseQuizResourceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseQuizResourceService {

	@Autowired
	private CourseQuizResourceRepository resourceRepository;


	public void deleteById(Long id) {
		resourceRepository.deleteById(id);
	}

	public Optional<CourseQuizResource> findByCourseIdAndName(long courseId, String fileName) {
		return resourceRepository.findByCourseIdAndName(courseId, fileName);
	}

	public void save(CourseQuizResource resource) {
		resourceRepository.save(resource);
	}
}
