package org.lecturestudio.web.portal.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@IdClass(DefaultRolePrivilegeId.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DefaultRolePrivilege {

	@Id
	@ManyToOne
	@JoinColumn(name = "role_id", referencedColumnName = "id", insertable = false, updatable = false)
	Role role;

	@Id
	@ManyToOne
	@JoinColumn(name = "privilege_id", referencedColumnName = "id", insertable = false, updatable = false)
	Privilege privilege;

	@Column
	Boolean enabled;

}
