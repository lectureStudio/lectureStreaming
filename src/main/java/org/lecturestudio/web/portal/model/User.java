package org.lecturestudio.web.portal.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

	@Column(name = "anonymousId", unique = true, nullable = false)
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

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "users_roles",
		joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
		inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	Set<Role> roles;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof User)) {
			return false;
		}

		User that = (User) o;

		return Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId);
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + "]";
	}
}
