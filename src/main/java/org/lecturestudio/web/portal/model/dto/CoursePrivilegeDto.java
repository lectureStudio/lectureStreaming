package org.lecturestudio.web.portal.model.dto;

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

    private Long id;

    private String name;

    private String descriptionKey;

}