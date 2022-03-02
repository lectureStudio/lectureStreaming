package org.lecturestudio.web.portal.model;

import java.util.Collection;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "course_privileges")
public class CoursePrivilege {

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	Long id;

    @Column(name = "privilege_name", unique = true, updatable = false, nullable = false)
    String name;

    @Column(name = "description_key")
    String descriptionKey;

    @ManyToMany(mappedBy = "privileges")
    Set<CourseUser> courseUsers;

    @ManyToMany(mappedBy = "privileges")
    Set<CourseRole> courseRoles;
}
