package org.lecturestudio.web.portal.model.janus;

import java.util.NoSuchElementException;

/**
 * Event types used to classify messages received from the Janus WebRTC Server
 * core.
 *
 * @author Alex Andres
 */
public enum JanusCoreEventType {

	/**
	 * Session created.
	 */
	CREATED("created"),

	/**
	 * Session created.
	 */
	DESTROYED("destroyed"),

	/**
	 * Session timeout.
	 */
	TIMEOUT("timeout");


	private final String type;


	JanusCoreEventType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public static JanusCoreEventType fromString(String typeStr) {
		for (var value : JanusCoreEventType.values()) {
			if (value.getType().equals(typeStr)) {
				return value;
			}
		}

		throw new NoSuchElementException("Not found: " + typeStr);
	}
}
