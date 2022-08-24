package org.lecturestudio.web.portal.config;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.lecturestudio.web.portal.model.ChatHistoryService;
import org.lecturestudio.web.portal.model.CourseStates;
import org.lecturestudio.web.portal.model.ScopedCoursePrivileges;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class ApplicationConfig {

	public interface ZonedDate {
		ZonedDateTime convert(ZonedDateTime dateTime);
	}



	@Bean
	public ChatHistoryService courseMessengerFeatureSaveFeature() {
		return new ChatHistoryService();
	}

	@Bean
	public CourseStates courseStates() {
		return new CourseStates();
	}

	@Bean
	@RequestScope
	public ScopedCoursePrivileges scopedCoursePrivileges() {
		return new ScopedCoursePrivileges();
	}

	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
		localeResolver.setDefaultLocale(Locale.US);

		return localeResolver;
	}

	@Bean(name = "zonedDateTime")
	public ZonedDate zonedDateTime() {
		return (ZonedDateTime dateTime) -> {
			return dateTime.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		};
	}
}
