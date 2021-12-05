package org.lecturestudio.web.portal.model.janus;

import java.math.BigInteger;

public abstract class JanusPluginEvent implements JanusEvent {

	private final BigInteger sessionId;


	/**
	 * Create a new {@code JanusPluginEvent} with the specified parameters.
	 *
	 * @param sessionId  The unique integer session ID.
	 */
	public JanusPluginEvent(BigInteger sessionId) {
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
}
