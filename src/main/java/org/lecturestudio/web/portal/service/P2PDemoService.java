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

import org.lecturestudio.web.portal.model.p2p.P2PDemoClient;
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

	private Map<String, List<P2PDemoClient>> clientMap = new ConcurrentHashMap<>();


	public void start(String userId, P2PDemoForm demoForm) {
		System.out.println(userId + " started " + demoForm.getNumPeers());

		List<P2PDemoClient> overlay = new ArrayList<>();

		Future<?> future = executorService.submit(new DemoTask(overlay, demoForm.getNumPeers()));

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

		final int LOW = 1000;
		final int HIGH = 10000;

		final List<P2PDemoClient> overlay;

		final Integer numClients;


		DemoTask(List<P2PDemoClient> overlay, Integer numClients) {
			this.overlay = overlay;
			this.numClients = numClients;
		}

		@Override
		public void run() {
			createClients();
			destroyClients();
		}

		private void createClients() {
			for (int i = 0; i < numClients; i++) {
				int sleepMs = random.nextInt(HIGH - LOW) + LOW;

				try {
					Thread.sleep(sleepMs);

					P2PDemoClient client = new P2PDemoClient();
					client.setUid(UUID.randomUUID());
					client.setBandwidth(random.nextInt(100) + 1);

					overlay.add(client);

					simpMessagingTemplate.convertAndSend("/topic/p2p/joined", client,
							Map.of("payloadType", client.getClass().getSimpleName()));
				}
				catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
		}

		private void destroyClients() {
			for (int i = 0; i < numClients; i++) {
				int sleepMs = random.nextInt(HIGH - LOW) + LOW;

				try {
					Thread.sleep(sleepMs);

					P2PDemoClient client = overlay.get(i);

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
}
