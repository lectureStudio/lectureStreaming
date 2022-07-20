package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.SpeechBaseMessage;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class SpeechMessageSerializer extends JsonSerializer<SpeechBaseMessage> {

	@Override
	public void serialize(SpeechBaseMessage message, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("type", message.getClass().getSimpleName());
		generator.writeNumberField("requestId", message.getRequestId());
		generator.writeObjectField("time", message.getDate());
		generator.writeStringField("firstName", message.getFirstName());
		generator.writeStringField("familyName", message.getFamilyName());
		generator.writeStringField("username", message.getRemoteAddress());
		generator.writeEndObject();
	}
}
