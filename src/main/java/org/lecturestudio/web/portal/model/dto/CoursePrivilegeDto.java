package org.lecturestudio.web.portal.model.dto;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePrivilegeDto {

	private String name;

	private String descriptionKey;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CoursePrivilegeDto)) {
			return false;
		}

		CoursePrivilegeDto that = (CoursePrivilegeDto) o;

		return Objects.equals(name, that.name)
			&& Objects.equals(descriptionKey, that.descriptionKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, descriptionKey);
	}
}