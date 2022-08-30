package org.lecturestudio.web.portal.model.p2p;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class P2PDemoForm {

	private Integer serverBandwidth;

	private Integer superPeerBandwidthThreshold;

	private Integer numServers;

	private Integer numPeers;

	private Integer numSuperPeers;

	private Integer maxSuperPeersClients;

	private Integer documentSize;

}
