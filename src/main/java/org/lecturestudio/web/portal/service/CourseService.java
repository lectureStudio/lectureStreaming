package org.lecturestudio.web.portal.service;

import static java.util.Objects.isNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.apache.commons.codec.digest.DigestUtils;

import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.CourseRoleId;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.PrivilegeFormDataSink;
import org.lecturestudio.web.portal.repository.CourseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

	@Autowired
	private CourseRepository repository;

	@Autowired
	private RoleService roleService;


	public Optional<Course> findById(Long id) {
		return repository.findById(id);
	}

	public Iterable<Course> findAllByUserId(String userId) {
		return repository.findAllByUserId(userId);
	}

	@Transactional
	public void deleteById(Long id) {
		roleService.deleteCourseRoleByCourse(id);
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

	public CourseForm getEmptyCourseForm() {
		List<Role> roles = Streamable.of(this.roleService.getAllRolesOrderedByIdAsc())
			.toList();

		List<CoursePrivilege> privileges = Streamable.of(this.roleService.getAllPrivilegesOrderByIdAsc())
			.toList();

		List<PrivilegeFormDataSink> privilegeSinks = new LinkedList<>();
		for (long i=0; i<roles.size(); i++) {
			for (CoursePrivilege privilege : privileges) {
				privilegeSinks.add(PrivilegeFormDataSink.builder()
										.privilege(privilege)
										.expressed(false)
										.build());					
			}
		}

		CourseForm form = new CourseForm();
		form.setCourseRoles(roles);
		form.setPrivilegeSinks(privilegeSinks);
		form.setNumOfPrivileges(privileges.size());

		return form;
	}

	public CourseForm getCourseForm(Course course) {
		List<Role> roles = Streamable.of(this.roleService.getAllRolesOrderedByIdAsc())
			.toList();

		List<CoursePrivilege> privileges = Streamable.of(this.roleService.getAllPrivilegesOrderByIdAsc())
			.toList();

		List<PrivilegeFormDataSink> privilegeSinks = new LinkedList<>();
		for (Role role : roles) {
			Optional<CourseRole> optCourseRole = this.roleService.findCourseRoleById(CourseRoleId.getIdFrom(course, role));
			Set<CoursePrivilege> expressedPrivileges = optCourseRole.isPresent() ? optCourseRole.get().getPrivileges() : new HashSet<>();
			for (CoursePrivilege privilege : privileges) {
				privilegeSinks.add(PrivilegeFormDataSink.builder()
										.privilege(privilege)
										.expressed(expressedPrivileges.contains(privilege))
										.build());			
			}
		}

		CourseForm form = new CourseForm();
		form.setId(course.getId());
		form.setTitle(course.getTitle());
		form.setDescription(course.getDescription());
		form.setPasscode(course.getPasscode());
		form.setRoomId(course.getRoomId());
		form.setCourseRoles(roles);
		form.setPrivilegeSinks(privilegeSinks);
		form.setNumOfPrivileges(privileges.size());
		return form;
	}
}
