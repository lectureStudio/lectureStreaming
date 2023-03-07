package org.lecturestudio.web.portal.admin.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.lecturestudio.web.portal.model.CourseState;
import org.lecturestudio.web.portal.model.CourseStateListener;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.service.CourseParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class CourseParticipantsCounter {

    @Autowired
    private CourseStates courseStates;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private CourseParticipantService participantService;

    private HashMap<Long, Gauge> participantsCounterOfCourse = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
        courseStates.addCourseStateListener(new CourseStateListener() {

            @Override
            public void courseStarted(long courseId, CourseState state) {
                CourseParticipantsCounter.this.courseStarted(courseId);
            }

            @Override
            public void courseEnded(long courseId, CourseState state) {
                CourseParticipantsCounter.this.courseEnded(courseId);
            }

        });
    }

    private void courseStarted(Long courseId) {
        Gauge gauge = Gauge
                .builder("course.participants", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return participantService.getNumOfUsersByCourseId(courseId);
                    }
                })
                .tag("course", courseId.toString())
                .register(meterRegistry);

        this.participantsCounterOfCourse.put(courseId, gauge);
    }

    private void courseEnded(Long courseId) {
        Gauge g = this.participantsCounterOfCourse.get(courseId);
        if (Objects.nonNull(g)) {
            meterRegistry.remove(g);
        }
        this.participantsCounterOfCourse.remove(courseId);
    }
}
