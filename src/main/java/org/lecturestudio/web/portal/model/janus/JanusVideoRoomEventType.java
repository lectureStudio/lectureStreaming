package org.lecturestudio.web.portal.model.janus;

import java.util.NoSuchElementException;

/**
 * Event types used to classify messages when communicating with the video-room
 * plugin that runs on the Janus WebRTC Server.
 *
 * @author Alex Andres
 */
public enum JanusVideoRoomEventType {

	/**
	 * Room created.
	 */
	CREATED("created"),

	/**
	 * Room destroyed.
	 */
	DESTROYED("destroyed"),

	/**
	 * Configured the room.
	 */
	CONFIGURED("configured"),

	/**
	 * Edited the room.
	 */
	EDITED("edited"),

	/**
	 * Kicked from the room.
	 */
	KICKED("kicked"),

	/**
	 * Joined the room.
	 */
	JOINED("joined"),

	/**
	 * Moderated the room.
	 */
	MODERATED("moderated"),

	/**
	 * Published media to the room.
	 */
	PUBLISHED("published"),

	/**
	 * Unpublished media in the room.
	 */
	UNPUBLISHED("unpublished"),

	/**
	 * Subscribing to a publisher.
	 */
	SUBSCRIBING("subscribing"),

	/**
	 * Subscribed to a publisher.
	 */
	SUBSCRIBED("subscribed"),

	/**
	 * Unsubscribed from a publisher.
	 */
	UNSUBSCRIBED("unsubscribed"),

	/**
	 * Leaving the room.
	 */
	LEAVING("leaving");


	private final String type;


	JanusVideoRoomEventType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public static JanusVideoRoomEventType fromString(String typeStr) {
		for (var value : JanusVideoRoomEventType.values()) {
			if (value.getType().equals(typeStr)) {
				return value;
			}
		}

		throw new NoSuchElementException("Not found: " + typeStr);
	}
}
