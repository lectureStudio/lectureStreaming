package org.lecturestudio.web.portal.admin.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "course_broadcasts")
public class CourseBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    Long id;

    @Column(nullable = false)
    Long courseId;

    @Column(nullable = false)
    LocalDateTime started;

    @Column(nullable = true)
    LocalDateTime ended;
}
