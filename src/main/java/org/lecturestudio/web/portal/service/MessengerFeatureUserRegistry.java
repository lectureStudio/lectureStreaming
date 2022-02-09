package org.lecturestudio.web.portal.service;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.lecturestudio.web.portal.saml.LectUserDetails;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class MessengerFeatureUserRegistry {

    private final ConcurrentHashMap<String, MessengerFeatureUser> users = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Set<MessengerFeatureUser>> usersOfCourse = new ConcurrentHashMap<>();



    public void onStompMessage(Message<?> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (!Objects.isNull(accessor)) {
            StompCommand command = accessor.getCommand();

            if (!Objects.isNull(command)) {
                switch(command) {
                    case CONNECT:
                        onConnectFrame(accessor);
                        break;
                    case DISCONNECT:
                        onDisconnectFrame(accessor);
                        break;
                    default:
                        return;
                }
            }
        }
    }

    private void onConnectFrame(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Long courseId = Long.parseLong(accessor.getNativeHeader("courseId").get(0));

        Principal user = accessor.getUser();
        LectUserDetails details = null;
        if (user instanceof UsernamePasswordAuthenticationToken) {
            details = (LectUserDetails) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
        }
        else {
            details = (LectUserDetails) ((KeycloakAuthenticationToken) user).getDetails();
        }
        String username = details.getUsername();

        MessengerFeatureUser messengerUser = this.users.get(username);
        if (Objects.isNull(messengerUser)) {
            messengerUser = new MessengerFeatureUser(username, user);
            this.users.put(username, messengerUser);
        }

        if (!messengerUser.hasSessions(courseId)) {
            Set<MessengerFeatureUser> uoc = this.usersOfCourse.get(courseId);
            if (Objects.isNull(uoc)) {
                uoc = new HashSet<>();
                this.usersOfCourse.put(courseId, uoc);
            }
            Set<MessengerFeatureUser> synSet = Collections.synchronizedSet(uoc);
            synSet.add(messengerUser);
        }

        MessengerFeatureSession session = new MessengerFeatureSession(courseId, sessionId, user.getName(), messengerUser);
        messengerUser.addSession(session);
    }

    private void onDisconnectFrame(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();

        Principal user = accessor.getUser();
        LectUserDetails details = null;
        if (user instanceof UsernamePasswordAuthenticationToken) {
            details = (LectUserDetails) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
        }
        else {
            details = (LectUserDetails) ((KeycloakAuthenticationToken) user).getDetails();
        }
        String username = details.getUsername();

        MessengerFeatureUser messengerUser = this.users.get(username);
        if (!Objects.isNull(messengerUser)) {
            MessengerFeatureSession s = messengerUser.getSession(sessionId);
            if (!Objects.isNull(s)) {
                Long courseId = s.getCourseId();
                messengerUser.removeSession(sessionId);

                if (!messengerUser.hasSessions()) {
                    this.users.remove(messengerUser.getUsername());
                    Set<MessengerFeatureUser> soc = this.usersOfCourse.get(courseId);
                    if (!Objects.isNull(soc)) {
                        Set<MessengerFeatureUser> synSoc = Collections.synchronizedSet(soc);
                        synSoc.remove(messengerUser);
                        
                    }
                }
                else if (!messengerUser.hasSessions(courseId)) {
                    Set<MessengerFeatureUser> soc = this.usersOfCourse.get(courseId);
                    if (!Objects.isNull(soc)) {
                        Set<MessengerFeatureUser> synSoc = Collections.synchronizedSet(soc);
                        synSoc.remove(messengerUser);
                    }
                }
            }
        }
    }

    public Set<MessengerFeatureUser> getUsers() {
        return new HashSet<>(this.users.values());
    }

    public Set<MessengerFeatureUser> getUsers(long courseId) {
        return this.usersOfCourse.get(courseId);
    }

    public MessengerFeatureUser getUser(String username) {
        return this.users.get(username);
    }

    @Override
    public String toString() {
        StringBuilder sbout = new StringBuilder();
        for (Entry<Long, Set<MessengerFeatureUser>> entry : this.usersOfCourse.entrySet()) {
            sbout.append("Course " + entry.getKey() + ":\n");
            StringBuilder sbin = new StringBuilder();
            for (MessengerFeatureUser u : entry.getValue()) {
                sbin.append(u.toString());
                sbin.append("\n");
            }
            sbout.append(sbin.toString().indent(5));
            sbout.append("\n");
        }
        return sbout.toString();
    }

    public class MessengerFeatureUser {

        private String username;

        private Principal principal;

        private final Set<String> addressesInUse = new HashSet<>();

        private final ConcurrentHashMap<String, MessengerFeatureSession> sessions = new ConcurrentHashMap<>();

        private final ConcurrentHashMap<Long, Set<MessengerFeatureSession>> sessionsForCourse = new ConcurrentHashMap<>();


        public MessengerFeatureUser(String username, Principal principal) {
            this.username = username;
            this.principal = principal;
        }

        public MessengerFeatureSession getSession(String sessionId) {
            return this.sessions.get(sessionId);
        }

        public String getUsername() {
            return username;
        }

        public Principal getPrincipal() {
            return principal;
        }

        public Set<String> getAddressesInUse() {
            return addressesInUse;
        }

        public Set<MessengerFeatureSession> getSessions() {
            return new HashSet<>(sessions.values());
        }

        public Set<MessengerFeatureSession> getSessions(Long courseId) {
            return new HashSet<>(sessionsForCourse.get(courseId));
        }

        public boolean hasSessions() {
            return ! sessions.isEmpty();
        }

        public boolean hasSessions(Long courseId) {
            Set<MessengerFeatureSession> sessions = sessionsForCourse.get(courseId);
            if (! Objects.isNull(sessions)) {
                return ! sessions.isEmpty();
            }
            return false;
        }

        public void addSession(MessengerFeatureSession session) {
            addressesInUse.add(session.getAddress());
            sessions.put(session.getSessionId(), session);
            Set<MessengerFeatureSession> sfc = sessionsForCourse.get(session.getCourseId());
            if (Objects.isNull(sfc)) {
                sfc = new HashSet<>();
                sessionsForCourse.put(session.getCourseId(), sfc);
            }
            synchronized(sfc) {
                sfc.add(session);
            }
        }

        public void removeSession(String sessionId) {
            MessengerFeatureSession session = sessions.get(sessionId);
            if (! Objects.isNull(session)) {
                String address = session.getAddress();
                Long courseId = session.getCourseId();

                this.sessions.remove(sessionId);
                boolean addressElsewhere = false;
                for (MessengerFeatureSession s : this.sessions.values()) {
                    if (s.getAddress().equals(address)) {
                        addressElsewhere = true;
                        break;
                    }
                }
                if (! addressElsewhere) {
                    this.addressesInUse.remove(address);
                }

                Set<MessengerFeatureSession> soc = this.sessionsForCourse.get(courseId);
                if (! Objects.isNull(soc)) {
                    synchronized(soc) {
                        soc.remove(session);
                    }
                }

            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("User " + this.username + " has following sessions:\n");
            for (MessengerFeatureSession s : this.sessions.values()) {
                sb.append(s.toString());
                sb.append("\n");
            }
            return sb.toString();
        }

    }

    public class MessengerFeatureSession {

        private Long courseId;

        private String sessionId;

        private String address;

        private MessengerFeatureUser user;


        public MessengerFeatureSession(Long courseId, String sessionId, String address, MessengerFeatureUser user) {
            this.courseId = courseId;
            this.sessionId = sessionId;
            this.address = address;
            this.user = user;
        }

        public String getAddress() {
            return address;
        }

        public Long getCourseId() {
            return courseId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public MessengerFeatureUser getUser() {
            return user;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Session " + this.sessionId + "=[ ");
            sb.append("courseId=" + this.courseId + ", ");
            sb.append("address=" + this.address);
            sb.append(" ]");
            return sb.toString();
        }
    }
    
}
