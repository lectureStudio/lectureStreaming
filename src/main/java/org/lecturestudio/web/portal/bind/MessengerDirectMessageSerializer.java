package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.MessengerDirectMessage;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class MessengerDirectMessageSerializer extends JsonSerializer<MessengerDirectMessage>{

    @Override
    public void serialize(MessengerDirectMessage message, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
                generator.writeStartObject();
                generator.writeStringField("_type", message.getClass().getSimpleName());
                generator.writeStringField("recipient", message.getRecipient());
                generator.writeStringField("text", message.getMessage().getText());
                generator.writeObjectField("time", message.getDate());
                generator.writeStringField("firstName", message.getFirstName());
                generator.writeStringField("familyName", message.getFamilyName());
                generator.writeStringField("userId", message.getUserId());
                generator.writeStringField("messageId", message.getMessageId());
                generator.writeBooleanField("reply", message.isReply());
                generator.writeEndObject();
    }

}