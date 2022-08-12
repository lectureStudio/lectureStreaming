package org.lecturestudio.web.portal.model.dto;

import org.lecturestudio.web.api.stream.model.CourseParticipantType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserDto {

	String userId;

	String firstName;

	String familyName;

	CourseParticipantType participantType;

}
