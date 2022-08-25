package org.lecturestudio.web.portal.model.p2p;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class P2PDemoClient {

	private UUID uid;

	private Integer bandwidth;

}
