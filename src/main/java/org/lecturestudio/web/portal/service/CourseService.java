package org.lecturestudio.web.portal.service;

import static java.util.Objects.isNull;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;

import org.lecturestudio.web.portal.exception.CourseNotFoundException;
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
import org.lecturestudio.web.portal.model.CourseForm.CourseFormUser;
import org.lecturestudio.web.portal.repository.CourseRepository;
import org.lecturestudio.web.portal.repository.CourseRoleRepository;
import org.lecturestudio.web.portal.repository.CourseUserRoleRepository;
import org.lecturestudio.web.portal.repository.PrivilegeRepository;
import org.lecturestudio.web.portal.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

	@Autowired
	private CourseRepository repository;

	@Autowired
	private CourseRoleRepository courseRoleRepository;

	@Autowired
	private CourseUserRoleRepository courseUserRoleRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PrivilegeRepository privilegeRepository;

	@Autowired
	private UserService userService;


	public Optional<Course> findById(Long courseId) {
		return repository.findById(courseId);
	}

	@Transactional
	public void deleteById(Long courseId) {
		repository.deleteById(courseId);
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

	public Set<Privilege> getUserPrivileges(Long courseId, String userId) {
		Course course = findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException());

		User user = userService.findById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("User could not be found!"));

		Set<CourseRole> courseRoles = course.getRoles();
		Set<Role> userRoles = user.getRoles();
		userRoles.addAll(courseUserRoleRepository.findAllRoles(courseId, user.getUserId()));

		Set<Privilege> userPrivileges = new HashSet<>();

		for (CourseRegistration registration : course.getRegistrations()) {
			if (registration.getUser().equals(user)) {
				// User is the owner of the course and thereby has all privileges.
				userPrivileges.addAll(getAllPossiblePrivileges());
				break;
			}
		}

		if (userPrivileges.isEmpty()) {
			for (CourseRole courseRole : courseRoles) {
				if (userRoles.contains(courseRole.getRole())) {
					for (CoursePrivilege coursePrivilege : courseRole.getPrivileges()) {
						userPrivileges.add(coursePrivilege.getPrivilege());
					}
				}
			}
		}

		return userPrivileges;
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
		List<CourseFormRole> formUserRoles = new ArrayList<>();

		for (Role role : roles) {
			List<CourseFormPrivilege> formPrivileges = new ArrayList<>();

			for (Privilege privilege : privileges) {
				formPrivileges.add(new CourseFormPrivilege(privilege, false));
			}

			formRoles.add(new CourseFormRole(role, formPrivileges));

			if (!role.getName().equals("participant")) {
				formUserRoles.add(new CourseFormRole(role, null));
			}
		}

		CourseForm form = new CourseForm();
		form.setRoles(formRoles);
		form.setUserRoles(formUserRoles);
		form.setNewUser(new CourseFormUser());
		form.setPrivilegedUsers(List.of());

		return form;
	}

	public CourseForm createCourseForm(Course course) {
		List<Privilege> privileges = privilegeRepository.findAll();

		List<CourseFormRole> formRoles = new ArrayList<>();
		List<CourseFormRole> formUserRoles = new ArrayList<>();

		for (CourseRole courseRole : course.getRoles()) {
			List<CourseFormPrivilege> formPrivileges = new ArrayList<>();
			Set<CoursePrivilege> coursePrivileges = courseRole.getPrivileges();

			for (Privilege privilege : privileges) {
				formPrivileges.add(new CourseFormPrivilege(privilege, coursePrivileges.stream()
						.filter(p -> p.getPrivilege().equals(privilege)).findFirst().isPresent()));
			}

			formRoles.add(new CourseFormRole(courseRole.getRole(), formPrivileges));

			if (!courseRole.getRole().getName().equals("participant")) {
				formUserRoles.add(new CourseFormRole(courseRole.getRole(), null));
			}
		}

		List<CourseFormUser> privilegedUsers = course.getUserRoles().stream()
				.map(userRole -> new CourseFormUser(userRole.getUsername(), userRole.getRole()))
				.collect(Collectors.toList());

		CourseForm form = new CourseForm();
		form.setId(course.getId());
		form.setRoomId(course.getRoomId());
		form.setTitle(course.getTitle());
		form.setDescription(course.getDescription());
		form.setPasscode(course.getPasscode());
		form.setRoles(formRoles);
		form.setUserRoles(formUserRoles);
		form.setNewUser(new CourseFormUser());
		form.setPrivilegedUsers(privilegedUsers);

		return form;
	}

	public boolean isAuthorized(long courseId, Authentication authentication, String operation)
			throws UnauthorizedException {
		UserDetails details = (UserDetails) authentication.getDetails();
		String username = details.getUsername();

		return isAuthorized(courseId, username, operation);
	}

	public boolean isAuthorized(long courseId, Principal principal, String operation)
			throws UnauthorizedException {
		return isAuthorized(courseId, principal.getName(), operation);
	}

	private boolean isAuthorized(long courseId, String userId, String operation)
			throws UnauthorizedException {
		Course course = findById(courseId)
				.orElseThrow(() -> new UnauthorizedException());

		User user = userService.findById(userId)
				.orElseThrow(() -> new UnauthorizedException());

		for (CourseRegistration registration : course.getRegistrations()) {
			if (registration.getUser().equals(user)) {
				// User is the owner of the course and thereby has all privileges.
				return true;
			}
		}

		Set<CourseRole> courseRoles = course.getRoles();
		Set<Role> userRoles = user.getRoles();
		userRoles.addAll(courseUserRoleRepository.findAllRoles(courseId, user.getUserId()));

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
