package org.lecturestudio.web.portal.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.lecturestudio.web.api.model.messenger.MessengerConfig.MessengerMode;

@Entity
public class CourseMessageFeature extends CourseFeature {

	@Enumerated(EnumType.STRING)
	private MessengerMode messengerMode;


	@Override
	public String getName() {
		return "messenger";
	}

	public MessengerMode getMessengerMode() {
		return this.messengerMode;
	}

	public void setMessengerMode(MessengerMode mode) {
		this.messengerMode = mode;
	}

}
