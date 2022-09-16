package org.lecturestudio.web.portal.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DefaultRolePrivilegeId implements Serializable {

	private Long role;

	private Long privilege;

}
