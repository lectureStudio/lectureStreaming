package org.lecturestudio.web.portal.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseSpeechRequest {

	@Id
	@GeneratedValue
	@Column(name = "id", updatable = false, nullable = false)
	Long id;

	@Column(nullable = false)
	String userId;

	@Column(nullable = false)
	Long requestId;

}
