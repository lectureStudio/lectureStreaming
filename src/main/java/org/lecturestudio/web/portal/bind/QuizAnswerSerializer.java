package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.QuizAnswerMessage;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class QuizAnswerSerializer extends JsonSerializer<QuizAnswerMessage> {

	@Override
	public void serialize(QuizAnswerMessage message, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("_type", message.getClass().getSimpleName());
		generator.writeObjectField("answer", message.getQuizAnswer());
		generator.writeObjectField("time", message.getDate());
		generator.writeEndObject();
	}
}
