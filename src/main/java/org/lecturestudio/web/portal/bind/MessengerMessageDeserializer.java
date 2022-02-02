package org.lecturestudio.web.portal.bind;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.model.Message;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class MessengerMessageDeserializer extends JsonDeserializer<MessengerMessage> {

    @Override
    public MessengerMessage deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        MessengerMessage message = new MessengerMessage();

        JsonNode treeNode = p.getCodec().readTree(p);
        
        if (treeNode.hasNonNull("firstName")) {
            message.setFirstName(treeNode.get("firstName").textValue());
        }

        if (treeNode.hasNonNull("familyName")) {
            message.setFamilyName(treeNode.get("familyName").textValue());
        }

        if (treeNode.hasNonNull("remoteAddress")) {
            message.setRemoteAddress(treeNode.get("remoteAddress").textValue());
        }

        if (treeNode.hasNonNull("date")) {
            message.setDate(ZonedDateTime.parse(treeNode.get("date").asText()));
        }

        if (treeNode.hasNonNull("messageId")) {
            message.setMessageId(treeNode.get("messageId").asText());
        }

        if (treeNode.hasNonNull("message")) {
            message.setMessage(new Message(treeNode.get("message").asText()));
        }

        if (treeNode.hasNonNull("reply")) {
            message.setReply(treeNode.get("reply").asBoolean());
        }
        
        return message;
    }
    
}
