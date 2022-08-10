package org.lecturestudio.web.portal.validator;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CourseForm.CourseFormUser;
import org.lecturestudio.web.portal.service.CourseRegistrationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class CourseUserRoleValidator {

	private interface ValidateFunction {

		void validate(CourseForm courseForm, Authentication authentication, BindingResult result);

	}


	private final ValidateFunction[] addValidators = {
			this::validateUserName,
			this::validateUserRole
	};

	@Autowired
	private CourseRegistrationService courseRegistrationService;

	@Autowired
	private Validator validator;

	@Autowired
	private MessageSource messageSource;


	public void validateNewUser(CourseForm courseForm, Authentication authentication, BindingResult result) {
		if (isNull(courseForm.getPrivilegedUsers())) {
			courseForm.setPrivilegedUsers(new ArrayList<>());
		}

		for (ValidateFunction func : addValidators) {
			func.validate(courseForm, authentication, result);
		}
	}

	public void validateRemoveUser(CourseForm courseForm, Authentication authentication, BindingResult result, int index) {
		List<CourseFormUser> privilegedUsers = courseForm.getPrivilegedUsers();
		CourseFormUser formUser = privilegedUsers.get(index);

		if (nonNull(formUser) && authentication.getName().equals(formUser.getUsername())) {
			result.reject("course.form.user.error.assigned.self");
		}
	}

	private void validateUserName(CourseForm courseForm, Authentication authentication, BindingResult result) {
		CourseFormUser newUser = courseForm.getNewUser();
		Long courseId = courseForm.getId();
		String userName = newUser.getUsername();

		boolean assignedOwner = false;

		if (nonNull(courseId)) {
			assignedOwner = courseRegistrationService.findByCourseAndUserId(courseId, userName).isPresent();
		}

		for (ConstraintViolation<CourseFormUser> violation : validator.validate(newUser)) {
			result.rejectValue("newUser.username", null, violation.getMessage());
		}

		if (authentication.getName().equals(userName)) {
			result.rejectValue("newUser.username", "course.form.user.error.assigned.self");
		}
		else if (assignedOwner) {
			result.rejectValue("newUser.username", "course.form.user.error.assigned.owner");
		}
	}

	private void validateUserRole(CourseForm courseForm, Authentication authentication, BindingResult result) {
		List<CourseFormUser> privilegedUsers = courseForm.getPrivilegedUsers();
		CourseFormUser newUser = courseForm.getNewUser();

		if (privilegedUsers.contains(newUser)) {
			CourseFormUser nUser = privilegedUsers.get(privilegedUsers.indexOf(newUser));

			String userName = newUser.getUsername();
			String roleName = messageSource.getMessage(nUser.getRole().getDescriptionKey(), null,
					LocaleContextHolder.getLocale());

			result.reject("course.form.user.error.role.duplicate",
					new String[] { userName, roleName }, "");
		}
	}
}
