package org.lecturestudio.web.portal.service;

import static java.util.Objects.isNull;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;

import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.repository.CourseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

	@Autowired
	private CourseRepository repository;


	public Optional<Course> findById(Long id) {
		return repository.findById(id);
	}

	public Iterable<Course> findAllByUserId(String userId) {
		return repository.findAllByUserId(userId);
	}

	public void deleteById(Long id) {
		repository.deleteById(id);
	}

	public Course saveCourse(Course course) {
		return repository.save(course);
	}

	public long getCourseCount() {
		return repository.count();
	}

	public Iterable<Course> getAllCourses() {
		return repository.findAll();
	}

	public Page<Course> getPaginated(final int pageNumber, final int pageSize, final String sortField,
			final String sortDirection) {
		final Sort sort = Sort.by(sortField).ascending();
		final Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

		return repository.findAll(pageable);
	}

	public String getHashedPasscode(Course course) {
		String passcode = course.getPasscode();

		if (isNull(passcode) || passcode.isEmpty() || passcode.isBlank()) {
			return null;
		}

		return DigestUtils.sha1Hex(passcode);
	}

	public boolean hasSameHashedPasscode(Course course, String passcode) {
		if (isNull(passcode) || passcode.isEmpty() || passcode.isBlank()) {
			return false;
		}

		return DigestUtils.sha1Hex(course.getPasscode()).equals(passcode);
	}
}
