package org.lecturestudio.web.portal.model.event;

import java.math.BigInteger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PeerEvent {

	Long courseId;

	BigInteger peerId;

	Boolean published;

}
