package org.lecturestudio.web.portal.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.CourseRoleId;
import org.lecturestudio.web.portal.model.PrivilegeFormDataSink;
import org.lecturestudio.web.portal.repository.RoleRepository;
import org.lecturestudio.web.portal.repository.CourseRoleRepository;
import org.lecturestudio.web.portal.repository.CoursePrivilegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

@Service
public class RoleService {
    
    @Autowired
    private CoursePrivilegeRepository coursePrivilegeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseRoleRepository courseRoleRepository;

    private final String ROLE_DATA_SOURCE = "classpath:data/data.json";



    /*
    *   Service methods for Course Privileges
    */
    public Optional<CoursePrivilege> findPrivilegeById(Long id) {
        return coursePrivilegeRepository.findById(id);
    }

    public Iterable<CoursePrivilege> getAllPrivileges() {
        return coursePrivilegeRepository.findAll();
    }

    public Iterable<CoursePrivilege> getAllPrivilegesOrderByIdAsc() {
        return coursePrivilegeRepository.findAllOrderedByIdAsc();
    }

    public CoursePrivilege saveCoursePrivilege(CoursePrivilege privilege) {
        return coursePrivilegeRepository.save(privilege);
    }

    public void deleteCoursePrivilege(CoursePrivilege privilege) {
        coursePrivilegeRepository.delete(privilege);
    }

    public void deleteCoursePrivilege(Long id) {
        coursePrivilegeRepository.deleteById(id);
    }

    public boolean hasCoursePrivilege(Long id) {
        return coursePrivilegeRepository.existsById(id);
    }

    public boolean hasCoursePrivilege(CoursePrivilege privilege) {
        return this.hasCoursePrivilege(privilege.getId());
    }



    /*
    *   Service methods for Roles
    */
    public Optional<Role> findRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<Role> findRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    public Iterable<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Iterable<Role> getAllRolesOrderedByIdAsc() {
        return roleRepository.findAllOrderedByIdAsc();
    }

    public Role saveRole(Role role) {
		return roleRepository.save(role);
	}

	public void deleteRole(Role role) {
		roleRepository.delete(role);
	}

    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

	public boolean hasRole(Long id) {
		return roleRepository.existsById(id);
	}

    public boolean hasRole(Role role) {
        return this.hasRole(role.getId());
    }

    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }



    /*
    *   Service methods for Course Roles
    */
    public Optional<CourseRole> findCourseRoleById(CourseRoleId id) {
        return courseRoleRepository.findById(id);
    }

    public Iterable<CourseRole> getAllCourseRoles() {
        return courseRoleRepository.findAll();
    }

    public CourseRole saveCourseRole(CourseRole courseRole) {
        return courseRoleRepository.save(courseRole);
    }

    public CourseRole saveCourseRole(Course course, Role role, Set<CoursePrivilege> privileges) {
        CourseRole courseRole = CourseRole.builder()
            .courseId(course.getId())
            .roleId(role.getId())
            .privileges(privileges)
            .build();

        return this.saveCourseRole(courseRole);
    }

    public void deleteCourseRole(CourseRoleId id) {
        this.courseRoleRepository.deleteById(id);
    }

    public void deleteCourseRole(CourseRole courseRole) {
        this.courseRoleRepository.deleteById(new CourseRoleId(courseRole.getCourseId(), courseRole.getRoleId()));
    }

    public void deleteCourseRoleByCourse(Long courseId) {
        courseRoleRepository.deleteCourseRoleByCourseId(courseId);
    }

    public void flushCourseFormRoles(Course course, CourseForm courseForm) {
        int base = 0;
		List<PrivilegeFormDataSink> dataSinks = courseForm.getPrivilegeSinks();

		for (Role role : courseForm.getCourseRoles()) {

			Set<CoursePrivilege> coursePrivileges = new HashSet<>();
			for (int i=0; i<courseForm.getNumOfPrivileges(); ++i) {
				PrivilegeFormDataSink current = dataSinks.get(base + i);
				if (current.isExpressed()) {
					coursePrivileges.add(current.getPrivilege());
				}
			}

			this.saveCourseRole(course, role, coursePrivileges);

			base += courseForm.getNumOfPrivileges();
		}
 
    }



    /*
    *   Service methods for loading initial data
    */
    public void loadInitialRoleData() {
        try {
            File data = ResourceUtils.getFile(this.ROLE_DATA_SOURCE);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<?, ?> dataMap = objectMapper.readValue(data, Map.class);

            ArrayList<?> privileges = (ArrayList) dataMap.get("privileges");
            ArrayList<?> roles = (ArrayList) dataMap.get("roles");

            Iterable<CoursePrivilege> iterablePrivileges = privileges.stream().map(obj -> {
                HashMap<?, ?> privilegeMap = (HashMap<?, ?>) obj;
                CoursePrivilege privilege = CoursePrivilege.builder()
                    .id(Long.valueOf(((Integer) privilegeMap.get("id")).longValue()))
                    .name((String) privilegeMap.get("name"))
                    .descriptionKey((String) privilegeMap.get("description"))
                    .build();
                
                return privilege;
            }).toList();

            coursePrivilegeRepository.saveAll(iterablePrivileges);

            Iterable<Role> iterableRoles = roles.stream().map(obj -> {
                HashMap<?, ?> roleMap = (HashMap<?, ?>) obj;

                String roleName = (String) roleMap.get("name");
                String roleNameLink = (String) roleMap.get("nameLink");

                Role role = Role.builder()
                    .name(roleName)
                    .nameLink(roleNameLink)
                    .build();
                
                return role;
            }).toList();

            roleRepository.saveAll(iterableRoles);

        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
