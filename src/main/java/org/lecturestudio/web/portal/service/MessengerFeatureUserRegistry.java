package org.lecturestudio.web.portal.service;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class MessengerFeatureUserRegistry {

    private final ConcurrentHashMap<String, MessengerFeatureUser> users = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Set<MessengerFeatureUser>> usersOfCourse = new ConcurrentHashMap<>();


    public void registerCourse(Long courseId) {
        usersOfCourse.putIfAbsent(courseId, new HashSet<>());
    }

    public void unregisterCourse(Long courseId) {
        usersOfCourse.remove(courseId);
    }

    public Set<MessengerFeatureUser> getUsers(long courseId) {
        return this.usersOfCourse.get(courseId);
    }

    public MessengerFeatureUser getUser(String username) {
        return this.users.get(username);
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

        public synchronized void addSession(MessengerFeatureSession session) {
            addressesInUse.add(session.getAddress());
            sessions.put(session.getSessionId(), session);
            Set<MessengerFeatureSession> sfc = sessionsForCourse.get(session.getCourseId());
            if (isNull(sfc)) {
                sfc = new HashSet<>();
                sessionsForCourse.put(session.getCourseId(), sfc);
            }
            Set<MessengerFeatureSession> syncedSfc = Collections.synchronizedSet(sfc);
            syncedSfc.add(session);
        }

        public synchronized void removeSession(String sessionId) {
            MessengerFeatureSession session = sessions.get(sessionId);
            if (nonNull(session)) {
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
                if (nonNull(soc)) {
                    Set<MessengerFeatureSession> syncedSoc = Collections.synchronizedSet(soc);
                    syncedSoc.remove(session);
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