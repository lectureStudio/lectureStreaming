package org.lecturestudio.web.portal.bind;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.lecturestudio.web.api.message.MessengerMessage;
import org.lecturestudio.web.api.message.MessengerReplyMessage;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class MessengerReplyMessageDeserializer extends JsonDeserializer<MessengerReplyMessage> {

    @Override
    public MessengerReplyMessage deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        MessengerMessage dummy = new MessengerMessage();
        
        JsonNode treeNode = p.getCodec().readTree(p);

        if (treeNode.hasNonNull("repliedMessageId")) {
            dummy.setMessageId(treeNode.get("repliedMessageId").asText());
        }

        MessengerReplyMessage message = new MessengerReplyMessage(dummy);

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

        return message;
    }
    
}
