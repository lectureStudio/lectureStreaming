package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.MessengerMessage;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class MessengerMessageSerializer extends JsonSerializer<MessengerMessage> {

	@Override
	public void serialize(MessengerMessage message, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("_type", message.getClass().getSimpleName());
		generator.writeStringField("text", message.getMessage().getText());
		generator.writeObjectField("time", message.getDate());
		generator.writeStringField("firstName", message.getFirstName());
		generator.writeStringField("familyName", message.getFamilyName());
		generator.writeEndObject();
	}
}
