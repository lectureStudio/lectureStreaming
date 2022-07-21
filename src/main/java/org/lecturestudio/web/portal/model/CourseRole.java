package org.lecturestudio.web.portal.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CourseRoleId.class)
public class CourseRole {

    @Id
    Long courseId;

    @Id
    Long roleId;

    @ManyToMany(fetch = FetchType.EAGER)
    Set<CoursePrivilege> privileges;

}