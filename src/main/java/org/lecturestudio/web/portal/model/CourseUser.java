package org.lecturestudio.web.portal.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CourseUserId.class)
public class CourseUser {

    @Id
    Long courseId;

    @Id
    String userId;

    @ManyToMany
    Set<CoursePrivilege> privileges;
    
}
