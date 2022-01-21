package org.lecturestudio.web.portal.config;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.lecturestudio.web.portal.model.CourseFeatureState;
import org.lecturestudio.web.portal.model.CourseMessengerFeatureSaveFeature;
import org.lecturestudio.web.portal.model.CourseStates;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class ApplicationConfig {

	public interface ZonedDate {
		ZonedDateTime convert(ZonedDateTime dateTime);
	}



	@Bean
	public CourseStates courseStates() {
		return new CourseStates();
	}

	@Bean
	public CourseFeatureState courseFeatureState() {
		return new CourseFeatureState();
	}

	@Bean
	public CourseMessengerFeatureSaveFeature courseMessengerFeatureSaveFeature() {
		return new CourseMessengerFeatureSaveFeature();
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
