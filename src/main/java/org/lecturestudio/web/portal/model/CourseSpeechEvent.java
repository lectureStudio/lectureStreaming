package org.lecturestudio.web.portal.model;

import java.math.BigInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class CourseSpeechEvent extends CourseEvent {

	BigInteger requestId;

	Boolean accepted;

}
