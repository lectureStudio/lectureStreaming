package org.lecturestudio.web.portal.model.dto;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserDto {

	String userId;

	String firstName;

	String familyName;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserDto)) {
			return false;
		}

		UserDto userDto = (UserDto) o;

		return Objects.equals(userId, userDto.userId)
			&& Objects.equals(firstName, userDto.firstName)
			&& Objects.equals(familyName, userDto.familyName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, firstName, familyName);
	}
}
