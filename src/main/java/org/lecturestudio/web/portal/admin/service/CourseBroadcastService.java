package org.lecturestudio.web.portal.admin.service;

import org.lecturestudio.web.portal.admin.model.CourseBroadcast;
import org.lecturestudio.web.portal.admin.repository.CourseBraodcastRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CourseBroadcastService {

    @Autowired
    private CourseBraodcastRepository courseBroadcastRepository;

    public Long saveCourseBroadcastStart(Long courseId, LocalDateTime started) {
        CourseBroadcast startedCourseBroadcast = CourseBroadcast.builder()
                .courseId(courseId)
                .started(started)
                .build();

        return courseBroadcastRepository.save(startedCourseBroadcast).getId();
    }

    public void saveCourseBroadcastEnd(Long courseBroadcastId, LocalDateTime ended) {
        Optional<CourseBroadcast> optCourseBroadcast = courseBroadcastRepository.findById(courseBroadcastId);

        if (optCourseBroadcast.isPresent()) {
            CourseBroadcast courseBroadcast = optCourseBroadcast.get();
            courseBroadcast.setEnded(ended);
            courseBroadcastRepository.save(courseBroadcast);
        }
    }
}
