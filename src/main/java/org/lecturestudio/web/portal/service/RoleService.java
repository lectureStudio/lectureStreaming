package org.lecturestudio.web.portal.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.portal.exception.UnauthorizedException;
import org.lecturestudio.web.portal.model.Course;
import org.lecturestudio.web.portal.model.CourseUser;
import org.lecturestudio.web.portal.model.CourseUserId;
import org.lecturestudio.web.portal.model.CourseForm;
import org.lecturestudio.web.portal.model.CoursePrivilege;
import org.lecturestudio.web.portal.model.Role;
import org.lecturestudio.web.portal.model.User;
import org.lecturestudio.web.portal.model.CourseRole;
import org.lecturestudio.web.portal.model.CourseRoleId;
import org.lecturestudio.web.portal.model.PrivilegeFormDataSink;
import org.lecturestudio.web.portal.repository.RoleRepository;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.repository.CourseUserRepository;
import org.lecturestudio.web.portal.repository.CourseRoleRepository;
import org.lecturestudio.web.portal.repository.CoursePrivilegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @Autowired
    private CourseUserRepository courseUserRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseRegistrationService courseRegistrationService;

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

    @Transactional
    public CourseRole saveCourseRole(CourseRole courseRole) {
        if (isConsistentWithDependencies(courseRole.getPrivileges())) {
            return courseRoleRepository.save(courseRole);
        }
        return null;
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



    /*
    *   Service methods for Course Users
    */
    public Optional<CourseUser> findCourseUserById(CourseUserId id) {
        return courseUserRepository.findById(id);
    }

    public Iterable<CourseUser> getAllCourseUsers() {
        return courseUserRepository.findAll();
    }

    public Iterable<CourseUser> getAllCourseUsersOfCourse(Course course) {
        return courseUserRepository.findByCourseId(course.getId());
    }

    @Transactional
    public CourseUser saveCourseUser(CourseUser courseUser) {
        if (isConsistentWithDependencies(courseUser.getPrivileges())) {
            return courseUserRepository.save(courseUser);
        }
        return null;
    }

    public CourseUser saveCourseUser(Course course, User user, Set<CoursePrivilege> privileges) {
        CourseUser courseUser = CourseUser.builder()
            .courseId(course.getId())
            .userId(user.getUserId())
            .privileges(privileges)
            .build();
        return this.saveCourseUser(courseUser);
    }

    public void deleteCourseUser(CourseUserId id) {
        this.courseUserRepository.deleteById(id);
    }

    public void deleteCourseUser(CourseUser courseUser) {
        this.courseUserRepository.deleteById(new CourseUserId(courseUser.getCourseId(), courseUser.getUserId()));
    }

    public void deleteCourseUserByCourse(Long courseId) {
        this.courseUserRepository.deleteCourseUserByCourseId(courseId);
    }

    public void deleteCourseUserByUser(String userId) {
        this.courseUserRepository.deleteCourseUserByUserId(userId);
    }

    public boolean hasCourseUser(CourseUserId courseUserId) {
        return this.courseUserRepository.existsById(courseUserId);
    }



    @Transactional
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

        for (User user : courseForm.getPersonallyPrivilegedUsers()) {

            Set<CoursePrivilege> coursePrivileges = new HashSet<>();
			for (int i=0; i<courseForm.getNumOfPrivileges(); ++i) {
				PrivilegeFormDataSink current = dataSinks.get(base + i);
				if (current.isExpressed()) {
					coursePrivileges.add(current.getPrivilege());
				}
			}

            this.saveCourseUser(course, user, coursePrivileges);

            base += courseForm.getNumOfPrivileges();
        }
 
    }



    /*
    *   Service methods for loading initial data
    */
    @Transactional
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

            coursePrivilegeRepository.deleteAll();
            coursePrivilegeRepository.saveAll(iterablePrivileges);

            for (HashMap<?,?> privMap : (ArrayList<HashMap<?,?>>) privileges) {
                if (privMap.containsKey("dependsOn")) {
                    Long id = Long.valueOf(((Integer) privMap.get("id")).longValue());
                    Long dependsOnId = Long.valueOf(((Integer) privMap.get("dependsOn")).longValue());
                    Optional<CoursePrivilege> optPrivilege = coursePrivilegeRepository.findById(id);
                    Optional<CoursePrivilege> optPrivilegeDepending = coursePrivilegeRepository.findById(dependsOnId);
                    if (optPrivilege.isPresent() && optPrivilegeDepending.isPresent()) {
                        CoursePrivilege privilege = optPrivilege.get();
                        CoursePrivilege privilegeDepending = optPrivilegeDepending.get();

                        privilege.setDependsOn(privilegeDepending);
                        coursePrivilegeRepository.save(privilege);
                    }
                }
            }

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

            roleRepository.deleteAll();
            roleRepository.saveAll(iterableRoles);

        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Transactional
    public void checkAuthorization(Course course, Authentication authentication, CoursePrivilege privilege) {
        LectUserDetails userDetails = (LectUserDetails) authentication.getDetails();
        String username = userDetails.getUsername();

        User user = userService.findById(username)
            .orElseThrow(() -> new UsernameNotFoundException("User with username " + username + " could not be found!"));

        System.out.println("Checking Authorization for user " + user.getUserId() + ":");
        System.out.println("Required Privilege: " + privilege.getName());

        if (!isCourseOwner(course, user)) {
        
            CourseUserId id = CourseUserId.getIdFrom(course, user);
            Set<CoursePrivilege> userPrivileges;
            if (hasCourseUser(id)) {
                CourseUser courseUser = findCourseUserById(id).get();
                userPrivileges = courseUser.getPrivileges();
            }
            else {
                Set<Role> userRoles = user.getRoles();
                userPrivileges = userRoles.stream()
                    .flatMap(role -> {
                        return findCourseRoleById(CourseRoleId.getIdFrom(course, role)).get().getPrivileges().stream();
                    }).collect(Collectors.toCollection(HashSet::new));
            }
            
            if (! userPrivileges.contains(privilege)) {
                System.out.println("Authorization REFUSED!");
                throw new UnauthorizedException("Authorization refused! User " + username + " does not have the required privilege " + privilege.getName());
            }
        }
        
        System.out.println("Authorization GRANTED!");
    }

    private boolean isCourseOwner(Course course, User user) {
        return courseRegistrationService.findByCourseAndUserId(course.getId(), user.getUserId()).isPresent();
    }

    private boolean isConsistentWithDependencies(Set<CoursePrivilege> coursePrivileges) {
        for (CoursePrivilege coursePrivilege : coursePrivileges) {
            CoursePrivilege dependsOn = coursePrivilege.getDependsOn();
            if (Objects.nonNull(dependsOn)) {
                if (! coursePrivileges.contains(dependsOn)) {
                    return false;
                }
            }
        }
        return true;
    }
}
