package org.lecturestudio.web.portal.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ConfigurationProperties(prefix = "simp")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SimpProperties {

	private Prefixes prefixes;

	private Destinations destinations;

	private Events events;

	private Endpoints endpoints;



	@ConfigurationProperties(prefix = "prefixes")
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	public static class Prefixes {

		private String[] app;

		private String[] broker;

		private String user;

	}



	@ConfigurationProperties(prefix = "destinations")
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	public static class Destinations {

		private String event;

	}



	@ConfigurationProperties(prefix = "events")
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	public static class Events {

		private String chat;

		private String quiz;

		private String presence;

		private String recording;

		private String stream;

		private String speech;

	}



	@ConfigurationProperties(prefix = "endpoints")
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	public static class Endpoints {

		private String publisher;

		private String state;

	}
}
