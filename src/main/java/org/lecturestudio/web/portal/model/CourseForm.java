package org.lecturestudio.web.portal.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseForm extends Course {

    @Transient
    private List<Role> courseRoles;

    @Transient
    private List<PrivilegeFormDataSink> privilegeSinks;

    @Transient
    private int numOfPrivileges;

    @Transient
    private List<User> personallyPrivilegedUsers;

    @Transient
    private String username;
    
}
