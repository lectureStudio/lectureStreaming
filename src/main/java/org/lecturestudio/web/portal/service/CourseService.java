package org.lecturestudio.web.portal.service;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseRegistration;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.Privilege;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.CourseForm.CourseFormPrivilege;
import org.lecturestudio.web.portal.model.CourseForm.CourseFormRole;
import org.lecturestudio.web.portal.repository.CourseRepository;
import org.lecturestudio.web.portal.repository.CourseRoleRepository;
import org.lecturestudio.web.portal.repository.PrivilegeRepository;
import org.lecturestudio.web.portal.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

	@Autowired
	private CourseRepository repository;

	@Autowired
	private CourseRoleRepository courseRoleRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PrivilegeRepository privilegeRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private CourseRegistrationService courseRegistrationService;


	public Optional<Course> findById(Long id) {
		return repository.findById(id);
	}

	@Transactional
	public void deleteById(Long id) {
		repository.deleteById(id);
	}

	public Course saveCourse(Course course) {
		return repository.save(course);
	}

	public CourseRole saveCourseRole(CourseRole courseRole) {
		return courseRoleRepository.save(courseRole);
	}

	public long getCourseCount() {
		return repository.count();
	}

	public Iterable<Course> getAllCourses() {
		return repository.findAll();
	}

	public List<Privilege> getAllPossiblePrivileges() {
		return privilegeRepository.findAll();
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

	public CourseForm createCourseForm() {
		List<Role> roles = roleRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
		List<Privilege> privileges = privilegeRepository.findAll();

		List<CourseFormRole> formRoles = new ArrayList<>();

		for (Role role : roles) {
			List<CourseFormPrivilege> formPrivileges = new ArrayList<>();

			for (Privilege privilege : privileges) {
				formPrivileges.add(new CourseFormPrivilege(privilege, false));
			}

			formRoles.add(new CourseFormRole(role, formPrivileges));
		}

		CourseForm form = new CourseForm();
		form.setRoles(formRoles);
		form.setUsername("");
		form.setPersonallyPrivilegedUsers(List.of());

		return form;
	}

	public CourseForm createCourseForm(Course course) {
		List<Privilege> privileges = privilegeRepository.findAll();

		List<CourseFormRole> formRoles = new ArrayList<>();

		for (CourseRole courseRole : course.getRoles()) {
			List<CourseFormPrivilege> formPrivileges = new ArrayList<>();
			Set<CoursePrivilege> coursePrivileges = courseRole.getPrivileges();

			for (Privilege privilege : privileges) {
				formPrivileges.add(new CourseFormPrivilege(privilege, coursePrivileges.stream()
						.filter(p -> p.getPrivilege().equals(privilege)).findFirst().isPresent()));
			}

			formRoles.add(new CourseFormRole(courseRole.getRole(), formPrivileges));
		}

		CourseForm form = new CourseForm();
		form.setId(course.getId());
		form.setRoomId(course.getRoomId());
		form.setTitle(course.getTitle());
		form.setDescription(course.getDescription());
		form.setPasscode(course.getPasscode());
		form.setRoles(formRoles);
		form.setUsername("");

		return form;
	}

	public boolean isAuthorized(long courseId, Authentication authentication, String operation)
			throws UnauthorizedException {
		UserDetails details = (UserDetails) authentication.getDetails();
		String username = details.getUsername();

		Course course = findById(courseId)
				.orElseThrow(() -> new UnauthorizedException());

		User user = userService.findById(username)
				.orElseThrow(() -> new UnauthorizedException());

		for (CourseRegistration registration : course.getRegistrations()) {
			if (registration.getUser().equals(user)) {
				// User is the owner of the course and thereby has all privileges.
				return true;
			}
		}

		Set<CourseRole> courseRoles = course.getRoles();
		Set<Role> userRoles = user.getRoles();

		for (CourseRole courseRole : courseRoles) {
			if (userRoles.contains(courseRole.getRole())) {
				for (CoursePrivilege coursePrivilege : courseRole.getPrivileges()) {
					if (coursePrivilege.getPrivilege().getName().equals(operation)) {
						return true;
					}
				}
			}
		}

		throw new UnauthorizedException();
	}
}
