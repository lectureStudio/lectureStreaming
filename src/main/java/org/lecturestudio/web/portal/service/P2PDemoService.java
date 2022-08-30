package org.lecturestudio.web.portal.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.lecturestudio.web.portal.model.p2p.P2PDemoPeer;
import org.lecturestudio.web.portal.model.p2p.P2PPeerType;
import org.lecturestudio.web.portal.model.p2p.P2PDemoForm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class P2PDemoService {

	private final ExecutorService executorService = Executors.newFixedThreadPool(3);

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	private Map<String, Future<?>> taskMap = new ConcurrentHashMap<>();

	private Map<String, List<P2PDemoPeer>> clientMap = new ConcurrentHashMap<>();


	public void start(String userId, P2PDemoForm demoForm) {
		System.out.println(userId + " started " + demoForm.getNumPeers());

		List<P2PDemoPeer> overlay = new ArrayList<>();

		Future<?> future = executorService.submit(new DemoTask(overlay, demoForm));

		if (taskMap.containsKey(userId)) {
			taskMap.get(userId).cancel(true);
		}

		clientMap.put(userId, overlay);
		taskMap.put(userId, future);
	}

	public void registerUser(String userId) {
		System.out.println("register user: " + userId);
	}

	public void unregisterUser(String userId) {
		System.out.println("unregister user: " + userId);

		if (taskMap.containsKey(userId)) {
			taskMap.get(userId).cancel(true);
		}
	}



	private class DemoTask implements Runnable {

		final SecureRandom random = new SecureRandom();

		final int MAX_PEER_BANDWIDTH = 200;
		final int LOW = 1000;
		final int HIGH = 5000;

		final P2PDemoForm demoForm;

		final List<P2PDemoPeer> overlay;
		final List<P2PDemoPeer> superPeers;

		final P2PDemoPeer server;

		final Integer numClients;
		final Integer numSuperClients;


		DemoTask(List<P2PDemoPeer> overlay, P2PDemoForm demoForm) {
			this.overlay = overlay;
			this.demoForm = demoForm;
			this.superPeers = new ArrayList<>();
			this.numClients = demoForm.getNumPeers();
			this.numSuperClients = demoForm.getNumSuperPeers();

			server = new P2PDemoPeer();
			server.setType(P2PPeerType.SERVER);
			server.setUid(UUID.fromString("19ba501f-cd70-42ad-855b-8423d0b5c4a2"));
			server.setBandwidth(demoForm.getServerBandwidth());
		}

		@Override
		public void run() {
			createClients();
			// destroyClients();
		}

		private void createClients() {
			for (int i = 0; i < numClients; i++) {
				int sleepMs = random.nextInt(HIGH - LOW) + LOW;

				try {
					Thread.sleep(sleepMs);

					P2PDemoPeer client = new P2PDemoPeer();
					client.setUid(UUID.randomUUID());
					client.setBandwidth(random.nextInt(MAX_PEER_BANDWIDTH) + 1);
					client.setServers(new ArrayList<>());

					if (client.getBandwidth() > demoForm.getSuperPeerBandwidthThreshold() && superPeers.size() < demoForm.getNumSuperPeers()) {
						client.setType(P2PPeerType.SUPER_PEER);
						client.getServers().addAll(List.of(server));

						superPeers.add(client);
					}
					else {
						client.setType(P2PPeerType.PEER);

						if (superPeers.isEmpty()) {
							client.getServers().addAll(List.of(server));
						}
						else {
							client.getServers().addAll(List.of(superPeers.get(random.nextInt(superPeers.size() - 1))));
						}
					}

					overlay.add(client);

					simpMessagingTemplate.convertAndSend("/topic/p2p/joined", client,
							Map.of("payloadType", client.getClass().getSimpleName()));

					executorService.execute(new DownloadTask(client, demoForm.getDocumentSize()));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void destroyClients() {
			for (int i = 0; i < numClients; i++) {
				int sleepMs = random.nextInt(HIGH - LOW) + LOW;

				try {
					Thread.sleep(sleepMs);

					P2PDemoPeer client = overlay.get(i);

					// overlay.remove(client);

					simpMessagingTemplate.convertAndSend("/topic/p2p/left", client,
							Map.of("payloadType", client.getClass().getSimpleName()));
				}
				catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
		}
	}



	private class DownloadTask implements Runnable {

		final P2PDemoPeer peer;

		final Double loadTime;


		DownloadTask(P2PDemoPeer peer, Integer docSize) {
			this.peer = peer;
			this.loadTime = docSize / (peer.getBandwidth() / 8.0);

			System.out.println(docSize + " " + peer.getBandwidth() + " " + loadTime);
		}

		@Override
		public void run() {
			try {
				Thread.sleep((long) (loadTime * 1000));

				simpMessagingTemplate.convertAndSend("/topic/p2p/document/done", peer,
							Map.of("payloadType", peer.getClass().getSimpleName()));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
