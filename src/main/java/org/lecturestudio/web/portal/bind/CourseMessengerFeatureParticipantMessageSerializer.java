package org.lecturestudio.web.portal.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.lecturestudio.web.api.message.CourseFeatureMessengerParticipantMessage;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class CourseMessengerFeatureParticipantMessageSerializer extends JsonSerializer<CourseFeatureMessengerParticipantMessage> {

    @Override
    public void serialize(CourseFeatureMessengerParticipantMessage message, JsonGenerator generator,
            SerializerProvider serializers) throws IOException {
        generator.writeStartObject();
		generator.writeStringField("_type", message.getClass().getSimpleName());
		generator.writeObjectField("time", message.getDate());
		generator.writeStringField("firstName", message.getFirstName());
		generator.writeStringField("familyName", message.getFamilyName());
		generator.writeStringField("remoteAddress", message.getRemoteAddress());
		generator.writeStringField("messageId", message.getMessageId());
		generator.writeBooleanField("connected", message.getConnected());
		generator.writeEndObject();

    }

}