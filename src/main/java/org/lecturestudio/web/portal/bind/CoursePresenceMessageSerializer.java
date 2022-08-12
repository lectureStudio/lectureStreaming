package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.CoursePresenceMessage;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class CoursePresenceMessageSerializer extends JsonSerializer<CoursePresenceMessage> {

	@Override
	public void serialize(CoursePresenceMessage message, JsonGenerator generator, SerializerProvider provider)
			throws IOException {
		generator.writeStartObject();
		generator.writeStringField("type", message.getClass().getSimpleName());
		generator.writeStringField("firstName", message.getFirstName());
		generator.writeStringField("familyName", message.getFamilyName());
		generator.writeStringField("userId", message.getUserId());
		generator.writeStringField("presence", message.getCoursePresence().toString());
		generator.writeStringField("presenceType", message.getCoursePresenceType().toString());
		generator.writeStringField("participantType", message.getCourseParticipantType().toString());
		generator.writeEndObject();
	}
}
