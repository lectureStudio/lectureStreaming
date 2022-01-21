package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.CourseParticipantMessage;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class CourseParticipantMessageSerializer extends JsonSerializer<CourseParticipantMessage> {

	@Override
	public void serialize(CourseParticipantMessage message, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("type", message.getClass().getSimpleName());
		generator.writeStringField("firstName", message.getFirstName());
		generator.writeStringField("familyName", message.getFamilyName());
		generator.writeStringField("username", message.getUsername());
		generator.writeBooleanField("connected", message.getConnected());
		generator.writeEndObject();
	}
}
