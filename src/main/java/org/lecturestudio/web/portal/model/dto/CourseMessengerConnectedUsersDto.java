package org.lecturestudio.web.portal.model.dto;

import java.util.Set;

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
public class CourseMessengerConnectedUsersDto {

    private Set<UserDto> connectedUsers;
    
}
