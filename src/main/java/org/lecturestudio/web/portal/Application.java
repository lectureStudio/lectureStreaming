package org.lecturestudio.web.portal;

import org.lecturestudio.web.portal.property.FileStorageProperties;
import org.lecturestudio.web.portal.property.SimpProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
	FileStorageProperties.class,
	SimpProperties.class
})
public class Application {

	public static void main(String... args) {
		SpringApplication.run(Application.class, args);
	}

}
