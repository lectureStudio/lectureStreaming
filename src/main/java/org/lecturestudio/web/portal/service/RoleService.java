package org.lecturestudio.web.portal.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lecturestudio.web.portal.exception.CourseNotFoundException;
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
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStateListener;
import org.lecturestudio.web.portal.model.PrivilegeFormDataSink;
import org.lecturestudio.web.portal.repository.RoleRepository;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.lecturestudio.web.portal.repository.CourseUserRepository;
import org.lecturestudio.web.portal.repository.CourseRoleRepository;
import org.lecturestudio.web.portal.repository.CoursePrivilegeRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import lombok.Getter;
import lombok.Setter;

@Service
public class RoleService implements CourseStateListener, InitializingBean {
    
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
    private CourseService courseService;

    @Autowired
    private CourseRegistrationService courseRegistrationService;


    private final Set<CoursePrivilege> coursePrivileges = ConcurrentHashMap.newKeySet();


    private final Set<Role> roles = ConcurrentHashMap.newKeySet();


    private final ConcurrentHashMap<Long, RoleService.CourseContext> courseContexts = new ConcurrentHashMap<>();

    
    private final String ROLE_DATA_SOURCE = "classpath:data/data.json";



    @Transactional
    @Override
    public void afterPropertiesSet() throws Exception {
        //Cache the privileges and roles in the sets coursePrivileges and roles
        coursePrivileges.addAll(Streamable.of(getAllPrivileges()).toSet());
        roles.addAll(Streamable.of(getAllRoles()).toSet());
    }





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
    @Transactional
    public Optional<CourseRole> findCourseRoleById(CourseRoleId id) {
        Course course = courseService.findById(id.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (isCourseStarted(course)) {
            CourseContext contextOfCourse = courseContexts.get(course.getId());
            Optional<CourseRole> optCourseRole = Optional.of(contextOfCourse
                                            .getCourseRoles()
                                            .get(id.getRoleId()));
            return optCourseRole;
        }
        return courseRoleRepository.findById(id);
    }

    public Iterable<CourseRole> getAllCourseRoles() {
        return courseRoleRepository.findAll();
    }

    @Transactional
    public Set<CourseRole> findCourseRoleByCourse(Course course) {
        courseService.findById(course.getId())
            .orElseThrow(() -> new CourseNotFoundException());


        if (isCourseStarted(course)) {
            CourseContext contextOfCourse = courseContexts.get(course.getId());
            return new HashSet<>(contextOfCourse.getCourseRoles().values());
        }
        return courseRoleRepository.findByCourseId(course.getId());
    } 

    @Transactional
    public CourseRole saveCourseRole(CourseRole courseRole) {
        Course course = courseService.findById(courseRole.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (isConsistentWithPrivilegeDependencies(courseRole.getPrivileges()) && !isCourseStarted(course)) {
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

    @Transactional
    public void deleteCourseRole(CourseRoleId id) {
        Course course = courseService.findById(id.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (!isCourseStarted(course)) {
            this.courseRoleRepository.deleteById(id);
        }
    }

    @Transactional
    public void deleteCourseRole(CourseRole courseRole) {
        Course course = courseService.findById(courseRole.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (!isCourseStarted(course)) {
            this.courseRoleRepository.deleteById(new CourseRoleId(courseRole.getCourseId(), courseRole.getRoleId()));
        }
    }

    @Transactional
    public void deleteCourseRoleByCourse(Long courseId) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException());

        if (!isCourseStarted(course)) {
            courseRoleRepository.deleteCourseRoleByCourseId(courseId);
        }
    }





    /*
    *   Service methods for Course Users
    */
    @Transactional
    public Optional<CourseUser> findCourseUserById(CourseUserId id) {
        Course course = courseService.findById(id.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (isCourseStarted(course)) {
            CourseContext contextOfCourse = courseContexts.get(course.getId());
            Optional<CourseUser> optCourseUser = Optional.of(contextOfCourse
                                            .getCourseUsers()
                                            .get(id.getUserId()));
            return optCourseUser;
        }
        return courseUserRepository.findById(id);
    }

    @Transactional
    public Set<CourseUser> findCourseUserByCourse(Course course) {
        courseService.findById(course.getId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (isCourseStarted(course)) {
            CourseContext contextOfCourse = courseContexts.get(course.getId());
            return new HashSet<>(contextOfCourse.getCourseUsers().values());
        }
        return courseUserRepository.findByCourseId(course.getId());
    }

    public Iterable<CourseUser> getAllCourseUsers() {
        return courseUserRepository.findAll();
    }

    @Transactional
    public CourseUser saveCourseUser(CourseUser courseUser) {
        Course course = courseService.findById(courseUser.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (isConsistentWithPrivilegeDependencies(courseUser.getPrivileges()) && !isCourseStarted(course)) {
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

    @Transactional
    public void deleteCourseUser(CourseUserId id) {
        Course course = courseService.findById(id.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (!isCourseStarted(course)) {
            this.courseUserRepository.deleteById(id);
        }
    }

    @Transactional
    public void deleteCourseUser(CourseUser courseUser) {
        Course course = courseService.findById(courseUser.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (! isCourseStarted(course)) {
            this.courseUserRepository.deleteById(new CourseUserId(courseUser.getCourseId(), courseUser.getUserId()));
        }
    }

    @Transactional
    public void deleteCourseUserByCourse(Long courseId) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException());
        if (!isCourseStarted(course)) {
            this.courseUserRepository.deleteCourseUserByCourseId(courseId);
        }
    }

    @Transactional
    public boolean hasCourseUser(CourseUserId courseUserId) {
        Course course = courseService.findById(courseUserId.getCourseId())
            .orElseThrow(() -> new CourseNotFoundException());

        if (isCourseStarted(course)) {
            return Objects.nonNull(courseContexts.get(course.getId()).getCourseUsers().get(courseUserId.getUserId()));
        }
        return this.courseUserRepository.existsById(courseUserId);
    }





    @Transactional
    public void flushCourseFormRoles(Course course, CourseForm courseForm) {
        int numOfPrivileges = courseForm.getNumOfPrivileges();
        Iterator<PrivilegeFormDataSink> iter = courseForm.getPrivilegeSinks().iterator();

        BiConsumer<Iterator<PrivilegeFormDataSink>, Set<CoursePrivilege>> consumePrivilegeDataSink = new BiConsumer<Iterator<PrivilegeFormDataSink>, Set<CoursePrivilege>>() {

            @Override
            public void accept(Iterator<PrivilegeFormDataSink> iter, Set<CoursePrivilege> coursePrivileges) {
                for (int i=0; i<numOfPrivileges; ++i) {
                    if (iter.hasNext()) {
                        PrivilegeFormDataSink current = iter.next();
                        if (current.isExpressed()) {
                            coursePrivileges.add(current.getPrivilege());
                        }
                    }
                    else {
                        throw new IndexOutOfBoundsException("There is not enough privilege forms for given roles and number of privileges");
                    }
                }
            }
        };

		for (Role role : courseForm.getCourseRoles()) {
			Set<CoursePrivilege> coursePrivileges = new HashSet<>();
            consumePrivilegeDataSink.accept(iter, coursePrivileges);
			this.saveCourseRole(course, role, coursePrivileges);
		}

        for (User user : courseForm.getPersonallyPrivilegedUsers()) {
            Set<CoursePrivilege> coursePrivileges = new HashSet<>();
			consumePrivilegeDataSink.accept(iter, coursePrivileges);
            this.saveCourseUser(course, user, coursePrivileges);
        }
    }





    /*
    *   Service methods for loading initial data
    */
    @Transactional
    public void loadInitialData() {
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

        courseService.findById(course.getId())
            .orElseThrow(() -> new CourseNotFoundException());

        User user = userService.findById(username)
            .orElseThrow(() -> new UsernameNotFoundException("User with username " + username + " could not be found!"));

        
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
            throw new UnauthorizedException("Authorization refused! User " + username + " does not have the required privilege " + privilege.getName());
        }
    }

    private boolean isCourseOwner(Course course, User user) {
        return courseRegistrationService.findByCourseAndUserId(course.getId(), user.getUserId()).isPresent();
    }

    @Transactional
    @Override
    public void courseStarted(long courseId, CourseState state) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException());

        CourseContext courseContext = new CourseContext(course);
        this.courseContexts.put(courseId, courseContext);
    }

    @Transactional
    @Override
    public void courseEnded(long courseId, CourseState state) {
        courseService.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException());
        
        this.courseContexts.remove(courseId);
    }

    public boolean isCourseStarted(Course course) {
        return Objects.nonNull(courseContexts.get(course.getId()));
    }

    private boolean isConsistentWithPrivilegeDependencies(Set<CoursePrivilege> coursePrivileges) {
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

    @Getter
    @Setter
    private class CourseContext {

        private final Course course;

        private final ConcurrentHashMap<Long, CourseRole> courseRoles = new ConcurrentHashMap();

        private final ConcurrentHashMap<String, CourseUser> courseUsers = new ConcurrentHashMap();

        private CourseContext(Course course) {
            this.course = course;

            Set<CourseRole> courseRoles = findCourseRoleByCourse(course);
            Set<CourseUser> courseUsers = findCourseUserByCourse(course);

            courseRoles.forEach((courseRole) -> {
                this.courseRoles.put(courseRole.getCourseId(), courseRole);
            });

            courseUsers.forEach((courseUser) -> {
                this.courseUsers.put(courseUser.getUserId(), courseUser);
            });
        }
    }
}
