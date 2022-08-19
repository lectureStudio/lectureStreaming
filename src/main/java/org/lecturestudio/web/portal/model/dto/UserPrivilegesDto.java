package org.lecturestudio.web.portal.model.dto;

import java.util.Set;

import org.lecturestudio.web.portal.model.Privilege;

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
public class UserPrivilegesDto {

	private Set<Privilege> privileges;

}
