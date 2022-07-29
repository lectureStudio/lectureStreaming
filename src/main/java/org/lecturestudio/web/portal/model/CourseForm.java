package org.lecturestudio.web.portal.model;

import java.util.List;

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

	private String title;

	private String description;

	private String passcode;

	private List<CourseFormRole> roles;

	private List<User> personallyPrivilegedUsers;

	private String username;



	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CourseFormRole {

		private Role role;

		private List<CourseFormPrivilege> privileges;

	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CourseFormPrivilege {

		private Privilege privilege;

		private boolean selected;

	}
}