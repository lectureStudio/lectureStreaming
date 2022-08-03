class PortalApp {

	constructor() {
		this.initialize();
	}

	courseStateChange(type, courseId, started) {
		const element = document.getElementById("course-" + type + "-" + courseId);

		if (!element) {
			return;
		}
		if (started) {
			element.classList.remove("d-none");
		}
		else {
			element.classList.add("d-none");
		}
	}

	initialize() {
		const client = new StompJs.Client({
			brokerURL: "wss://" + window.location.host + "/ws-state",
			reconnectDelay: 1000,
			heartbeatIncoming: 1000,
			heartbeatOutgoing: 1000,
		});
		client.onConnect = () => {
			client.subscribe("/topic/course/event/all/stream", (message) => {
				const state = JSON.parse(message.body);

				this.courseStateChange("live", state.courseId, state.started);

				if (!state.started) {
					this.courseStateChange("recording", state.courseId, false);
				}
			});
			client.subscribe("/topic/course/event/all/recording", (message) => {
				const state = JSON.parse(message.body);

				this.courseStateChange("recording", state.courseId, state.started);
			});
			client.subscribe("/topic/course/event/all/chat", (message) => {
				const state = JSON.parse(message.body);

				this.courseStateChange("messenger", state.courseId, state.started);
			});
			client.subscribe("/topic/course/event/all/quiz", (message) => {
				const state = JSON.parse(message.body);

				this.courseStateChange("quiz", state.courseId, state.started);
			});
		};
		client.activate();

		window.addEventListener("beforeunload", () => {
			client.deactivate();
		});
	}
}

class ClipboardCopyElement extends HTMLButtonElement {

	constructor() {
		super();

		this.addEventListener("click", e => this.copy());
	}

	copy() {
		if (this.hasAttribute("for")) {
			const forId = this.getAttribute("for");
			const element = document.getElementById(forId);
			const clipboard = navigator.clipboard;
			let value = null;

			if (element instanceof HTMLInputElement) {
				value = element.value;
			}
			else {
				value = element.innerText;
			}

			clipboard.writeText(value);
		}
	}
}

class PasswordVisibilityElement extends HTMLButtonElement {

	constructor() {
		super();
		this.addEventListener("click", e => this.toggle());
	}

	toggle() {
		if (this.hasAttribute("for")) {
			const forId = this.getAttribute("for");
			const visibleClass = this.getAttribute("data-class-visible");
			const hiddenClass = this.getAttribute("data-class-hidden");
			const input = document.getElementById(forId);
			const icon = document.getElementById("visibility-icon");

			if (input.type === "password") {
				input.type = "text";

				if (icon) {
					icon.classList.remove(hiddenClass);
					icon.classList.add(visibleClass);
				}
			}
			else {
				input.type = "password";

				if (icon) {
					icon.classList.remove(visibleClass);
					icon.classList.add(hiddenClass);
				}
			}
		}
	}
}

customElements.define("clipboard-copy", ClipboardCopyElement, { extends: "button" });
customElements.define("password-visibility", PasswordVisibilityElement, { extends: "button" });


window.portalApp = new PortalApp();


function enumerateDevices(useVideo, useSettings) {
	let constraints;

	if (useSettings) {
		const audioSource = localStorage.getItem("audioinput");
		const videoSource = localStorage.getItem("videoinput");
	
		constraints = {
			audio: {
				deviceId: audioSource ? { exact: audioSource } : undefined
			},
			video: {
				deviceId: videoSource ? { exact: videoSource } : undefined,
				width: 1280,
				height: 720,
				facingMode: "user"
			}
		};
	
		if (!useVideo) {
			delete constraints.video;
		}
	}
	else {
		constraints = {
			audio: true,
			video: {
				width: 1280,
				height: 720
			}
		};
	}

	return navigator.mediaDevices.getUserMedia(constraints)
		.then(stream => {
			return navigator.mediaDevices.enumerateDevices()
				.then(devices => {
					const result = {
						devices: devices,
						stream: stream,
						constraints: constraints
					};

					return result;
				})
				.catch(error => {
					console.error(error);
				});
		});
}

function clearDeviceChoice() {
	localStorage.removeItem("audioinput");
	localStorage.removeItem("audiooutput");
	localStorage.removeItem("videoinput");
}

function saveDeviceChoice(devices) {
	if (devices.audioInput) {
		if (devices.audioInput === "none") {
			localStorage.removeItem("audioInput");
		}
		else {
			localStorage.setItem("audioinput", devices.audioInput);
		}
	}
	if (devices.audioOutput) {
		if (devices.audioOutput === "none") {
			localStorage.removeItem("audioOutput");
		}
		else {
			localStorage.setItem("audiooutput", devices.audioOutput);
		}
	}
	if (devices.videoInput) {
		if (devices.videoInput === "none") {
			localStorage.removeItem("videoinput");
		}
		else {
			localStorage.setItem("videoinput", devices.videoInput);
		}
	}
}

function getAudioLevel(audioTrack, canvas) {
	const meterContext = canvas.getContext("2d");

	pollAudioLevel(audioTrack, (level) => {
		meterContext.fillStyle = "lightgrey";
		meterContext.fillRect(0, 0, canvas.width, canvas.height);
		meterContext.fillStyle = "#0d6efd";
		meterContext.fillRect(0, 0, level * canvas.width, canvas.height);
	});
}

function createAudioMeter(audioContext, clipLevel, averaging, clipLag) {
	const processor = audioContext.createScriptProcessor(512);
	processor.clipping = false;
	processor.lastClip = 0;
	processor.volume = 0;
	processor.clipLevel = clipLevel || 0.98;
	processor.averaging = averaging || 0.95;
	processor.clipLag = clipLag || 750;
	processor.onaudioprocess = function(event) {
		const inputBuffer = event.inputBuffer;
		let rmsSum = 0;
	
		for (let c = 0; c < inputBuffer.numberOfChannels; c++) {
			const inputData = inputBuffer.getChannelData(c);
			const bufLength = inputData.length;
			let sum = 0;
			let x;
	
			for (var i = 0; i < bufLength; i++) {
				x = inputData[i];
	
				if (Math.abs(x) >= this.clipLevel) {
					this.clipping = true;
					this.lastClip = window.performance.now();
				}
	
				sum += x * x;
			}
	
			rmsSum += Math.sqrt(sum / bufLength);
		}
	
		this.volume = Math.max(rmsSum / 2, this.volume * this.averaging);
	};
	processor.checkClipping = function() {
		if (!this.clipping) {
			return false;
		}
		if ((this.lastClip + this.clipLag) < window.performance.now()) {
			this.clipping = false;
		}

		return this.clipping;
	};
	processor.shutdown = function() {
		this.disconnect();
		this.onaudioprocess = null;
	};

	return processor;
}

async function pollAudioLevel(track, onLevelChanged) {
	const audioContext = new AudioContext();

	// Due to browsers' autoplay policy.
	await audioContext.resume();

	// const stream = new MediaStream([track]);
	// const mediaStreamSource = audioContext.createMediaStreamSource(stream);
	// const meter = createAudioMeter(audioContext);

	// mediaStreamSource.connect(meter);

	const analyser = audioContext.createAnalyser();
	analyser.minDecibels = -127;
	analyser.maxDecibels = 0;
	analyser.fftSize = 1024;
	analyser.smoothingTimeConstant = 0.5;

	const stream = new MediaStream([track]);
	const source = audioContext.createMediaStreamSource(stream);
	source.connect(analyser);

	const samples = new Uint8Array(analyser.frequencyBinCount);

	function rootMeanSquare(samples) {
		const sumSq = samples.reduce((sumSq, sample) => sumSq + sample, 0);
		return sumSq / samples.length;
	}

	requestAnimationFrame(function checkLevel() {
		// onLevelChanged(meter.volume);

		analyser.getByteFrequencyData(samples);

		const level = rootMeanSquare(samples) / 255;

		onLevelChanged(Math.max(Math.min(level, 1), 0));

		// Continue calculating the level only if the audio track is live.
		if (track.readyState === "live") {
			requestAnimationFrame(checkLevel);
		}
		else {
			requestAnimationFrame(() => onLevelChanged(0));
		}
	});
}

function removeHwId(label) {
	const matches = label.match(/\s\([a-zA-Z0-9]{4}:[a-zA-Z0-9]{4}\)/g);
	let hwId = null;

	if (matches && matches.length > 0) {
		hwId = matches[0];
	}

	return hwId ? label.replace(hwId, "") : label;
}

function stopAudioTracks(stream) {
	if (stream) {
		stream.getAudioTracks().forEach(track => {
			track.stop();
		});
	}
}

function stopVideoTracks(stream) {
	if (stream) {
		stream.getVideoTracks().forEach(track => {
			track.stop();
		});
	}
}

function stopMediaTracks(stream) {
	if (stream) {
		stream.getTracks().forEach(track => {
			track.stop();
		});
	}
}

function setAudioSink(mediaElement, sinkId) {
	if (!('sinkId' in HTMLMediaElement.prototype)) {
		return;
	}

	mediaElement.setSinkId(sinkId)
		.catch(error => {
			console.error(error);
		});
}

function removeAllChildNodes(parent) {
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
}