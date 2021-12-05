package org.lecturestudio.web.portal.model.janus;

import java.math.BigInteger;

public class JanusCoreEvent implements JanusEvent {

	private final BigInteger sessionId;

	private final JanusCoreEventType eventType;


	/**
	 * Create a new {@code JanusCoreEvent} with the specified parameters.
	 *
	 * @param sessionId The unique integer session ID.
	 * @param eventType The event type.
	 */
	public JanusCoreEvent(JanusCoreEventType eventType, BigInteger sessionId) {
		this.eventType = eventType;
		this.sessionId = sessionId;
	}

	/**
	 * Get the unique session ID.
	 *
	 * @return The unique integer session ID.
	 */
	public BigInteger getSessionId() {
		return sessionId;
	}

	/**
	 * Get the type of this event.
	 *
	 * @return The event type.
	 */
	public JanusCoreEventType getEventType() {
		return eventType;
	}
}
