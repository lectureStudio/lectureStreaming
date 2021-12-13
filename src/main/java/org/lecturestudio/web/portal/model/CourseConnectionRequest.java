package org.lecturestudio.web.portal.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseConnectionRequest {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    Long id;

    @Column(nullable = false)
    String userId;

    @Column
    String firstName;

    @Column
    String familyName;

    @Column
    long courseId;

    @Column(nullable = false)
    Long requestId;
}
