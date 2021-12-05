package org.lecturestudio.web.portal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseEmitters {

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();


	public SseEmitter add(SseEmitter emitter) {
		emitters.add(emitter);

		emitter.onCompletion(() -> {
			emitters.remove(emitter);
		});
		emitter.onTimeout(() -> {
			emitter.complete();
			emitters.remove(emitter);
		});

		return emitter;
	}

	public void send(Object obj) {
		List<SseEmitter> failedEmitters = new ArrayList<>();

		emitters.forEach(emitter -> {
			try {
				emitter.send(obj);
			}
			catch (Exception e) {
				emitter.completeWithError(e);
				failedEmitters.add(emitter);
			}
		});

		emitters.removeAll(failedEmitters);
	}

}
