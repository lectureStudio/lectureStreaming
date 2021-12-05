package org.lecturestudio.web.portal.bind;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.lecturestudio.web.portal.model.janus.JanusCoreEvent;
import org.lecturestudio.web.portal.model.janus.JanusCoreEventType;
import org.lecturestudio.web.portal.model.janus.JanusEvent;
import org.lecturestudio.web.portal.model.janus.JanusVideoRoomEvent;
import org.lecturestudio.web.portal.model.janus.JanusVideoRoomEventType;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class JanusEventDeserializer extends JsonDeserializer<JanusEvent> {

	@Override
	public JanusEvent deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = p.getCodec().readTree(p);
// System.out.println(node.toPrettyString());
		if (!node.hasNonNull("type")) {
			return null;
		}

		final int eventType = node.get("type").asInt();
		final JsonNode eventNode = node.path("event");

		if (eventNode.isMissingNode()) {
			return null;
		}

		if (eventType == 1) {
			// Core message.
			if (!eventNode.hasNonNull("name")) {
				return null;
			}

			final JanusCoreEventType type = JanusCoreEventType.fromString(eventNode.get("name").asText());

			// Only interested in "session created/destroyed".
			switch (type) {
				case CREATED:
				case DESTROYED:
					return new JanusCoreEvent(type, getSessionId(node));

				default:
					return null;
			}
		}
		else if (eventType == 64) {
			// Plugin message.
			if (!eventNode.hasNonNull("plugin")) {
				return null;
			}

			String pluginName = eventNode.get("plugin").textValue();

			if (pluginName.equals(JanusVideoRoomEvent.PLUGIN_NAME)) {
				return createVideoRoomEvent(getSessionId(node), eventNode);
			}
		}

		return null;
	}

	private static BigInteger getSessionId(JsonNode node) {
		if (node.hasNonNull("session_id")) {
			return node.get("session_id").bigIntegerValue();
		}

		return null;
	}

	private static JanusEvent createVideoRoomEvent(BigInteger sessionId, JsonNode node) {
		final JsonNode data = node.path("data");

		if (!data.hasNonNull("event") || !data.hasNonNull("room")) {
			return null;
		}

		final JanusVideoRoomEventType type = JanusVideoRoomEventType.fromString(data.get("event").asText());
		final BigInteger roomId = data.get("room").bigIntegerValue();
		BigInteger peerId = null;

		if (data.hasNonNull("id")) {
			peerId = data.get("id").bigIntegerValue();
		}

		switch (type) {
			case SUBSCRIBED:
			case UNSUBSCRIBED:
			case PUBLISHED:
				return new JanusVideoRoomEvent(type, sessionId, roomId, peerId);

			default:
				return null;
		}
	}
}
