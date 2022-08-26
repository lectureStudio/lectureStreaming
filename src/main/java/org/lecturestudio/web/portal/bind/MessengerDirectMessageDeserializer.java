package org.lecturestudio.web.portal.bind;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.lecturestudio.web.api.message.MessengerDirectMessage;
import org.lecturestudio.web.api.model.Message;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class MessengerDirectMessageDeserializer extends JsonDeserializer<MessengerDirectMessage> {

	@Override
	public MessengerDirectMessage deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		MessengerDirectMessage message = new MessengerDirectMessage();

		JsonNode treeNode = p.getCodec().readTree(p);

		if (treeNode.hasNonNull("userId")) {
			message.setUserId(treeNode.get("userId").textValue());
		}
		if (treeNode.hasNonNull("firstName")) {
			message.setFirstName(treeNode.get("firstName").textValue());
		}
		if (treeNode.hasNonNull("familyName")) {
			message.setFamilyName(treeNode.get("familyName").textValue());
		}
		if (treeNode.hasNonNull("recipientId")) {
			message.setRecipientId(treeNode.get("recipientId").textValue());
		}
		if (treeNode.hasNonNull("recipientFirstName")) {
			message.setRecipientFirstName(treeNode.get("recipientFirstName").textValue());
		}
		if (treeNode.hasNonNull("recipientFamilyName")) {
			message.setRecipientFamilyName(treeNode.get("recipientFamilyName").textValue());
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

		return message;
	}

}