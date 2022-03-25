package org.lecturestudio.web.portal.bind;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.lecturestudio.web.api.message.EmojiMessage;
import org.lecturestudio.web.api.message.SpeechBaseMessage;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class EmojiMessageSerializer extends JsonSerializer<EmojiMessage> {

	@Override
	public void serialize(EmojiMessage message, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("type", message.getClass().getSimpleName());
		generator.writeObjectField("emoji", message.getEmoji());
		generator.writeObjectField("time", message.getDate());
		generator.writeStringField("username", message.getRemoteAddress());
		generator.writeEndObject();
	}
}
