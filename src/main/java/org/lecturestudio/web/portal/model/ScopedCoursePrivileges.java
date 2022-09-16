package org.lecturestudio.web.portal.model;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ScopedCoursePrivileges {

	private Set<Privilege> privileges;

}
