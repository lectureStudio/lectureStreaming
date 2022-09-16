package org.lecturestudio.web.portal.model;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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



	@Data
	@Builder
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CourseFormUser {

		@NotNull
		@Size(min = 5, max = 20, message = "{course.form.user.error.username.invalid}")
		private String username;

		@EqualsAndHashCode.Exclude
		private String firstName;

		@EqualsAndHashCode.Exclude
		private String familyName;

		@NotNull
		private Role role;


		public static int compareByUserName(CourseFormUser lhs, CourseFormUser rhs) {
			return lhs.getUsername().compareTo(rhs.getUsername());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof CourseFormUser)) {
				return false;
			}

			CourseFormUser that = (CourseFormUser) o;

			return Objects.equals(username, that.username);
		}

		@Override
		public int hashCode() {
			return Objects.hash(username);
		}
	}
}