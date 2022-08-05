package org.lecturestudio.web.portal.model;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseForm {

	private Long id;

	private String roomId;

	@NotNull
	@Size(min = 3, max = 200)
	private String title;

	private String description;

	private String passcode;

	private List<CourseFormRole> roles;

	private List<CourseFormRole> userRoles;

	private List<CourseFormUser> privilegedUsers;

	private CourseFormUser newUser;



	



	@Override
	public String toString() {
		return "CourseForm [description=" + description + ", id=" + id + ", passcode=" + passcode + ", privilegedUsers="
				+ privilegedUsers + ", roles=" + roles + ", title=" + title + ", userRoles=" + userRoles + "]";
	}



	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CourseFormRole {

		private Role role;

		private List<CourseFormPrivilege> privileges;

		@Override
		public String toString() {
			return "CourseFormRole [privileges=" + privileges + ", role=" + role + "]";
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CourseFormPrivilege {

		private Privilege privilege;

		private boolean selected;

	}



	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CourseFormUser {

		private String username;

		private Role role;

		@Override
		public String toString() {
			return "CourseFormUser [role=" + role + ", username=" + username + "]";
		}
	}
}