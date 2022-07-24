package org.lecturestudio.web.portal.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "users")
public class User {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	String userId;

	@Column(name = "anonymousId", unique = true, updatable = false, nullable = false)
	// @Type(type = "org.hibernate.type.UUIDCharType")
	UUID anonymousUserId;

	@Column(name = "first_name", updatable = false, nullable = false)
	String firstName;

	@Column(name = "family_name", updatable = false, nullable = false)
	String familyName;

	@UpdateTimestamp
	LocalDateTime lastModified;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "token_id", referencedColumnName = "id")
	PersonalToken token;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	Set<CourseRegistration> registrations;

	@ManyToMany
	Set<Role> roles;

}
