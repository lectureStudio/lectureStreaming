package org.lecturestudio.web.portal.model.janus;

import java.math.BigInteger;

public class JanusVideoRoomEvent extends JanusPluginEvent {

	public static final String PLUGIN_NAME = "janus.plugin.videoroom";

	private final JanusVideoRoomEventType eventType;

	private final BigInteger roomId;

	private final BigInteger peerId;

	private String opaqueId;


	/**
	 * Create a new {@code JanusVideoRoomEvent} with the specified parameters.
	 *
	 * @param eventType The event type.
	 * @param sessionId The unique integer session ID.
	 * @param roomId    The unique numeric room ID.
	 * @param peerId    The unique numeric peer ID.
	 */
	public JanusVideoRoomEvent(JanusVideoRoomEventType eventType, BigInteger sessionId, BigInteger roomId, BigInteger peerId) {
		super(sessionId);

		this.eventType = eventType;
		this.roomId = roomId;
		this.peerId = peerId;
	}

	/**
	 * Get the type of this event.
	 *
	 * @return The event type.
	 */
	public JanusVideoRoomEventType getEventType() {
		return eventType;
	}

	/**
	 * Get the unique numeric room ID this event belongs to.
	 *
	 * @return The unique numeric room ID.
	 */
	public BigInteger getRoomId() {
		return roomId;
	}

	/**
	 * Get the unique numeric peer ID this event belongs to.
	 *
	 * @return The unique numeric peer ID.
	 */
	public BigInteger getPeerId() {
		return peerId;
	}

	/**
	 * Get the opaque ID this event belongs to.
	 *
	 * @return The opaque ID.
	 */
	public String getOpaqueId() {
		return opaqueId;
	}

	/**
	 * Set the opaque ID this event belongs to.
	 *
	 * @param id The opaque ID.
	 */
	public void setOpaqueId(String id) {
		this.opaqueId = id;
	}
}
