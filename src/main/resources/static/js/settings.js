let mediaRecorder = null;
let deviceStream = null;
let audioInputStream = null;
let videoInputStream = null;
let chunks = [];

function initMediaProfile() {
	const storedMediaProfile = localStorage.getItem("media.profile");
	const mediaProfiles = document.querySelectorAll('input[name = "mediaProfile"]');
	let selected = false;

	for (const input of mediaProfiles) {
		if (input.value === storedMediaProfile) {
			input.checked = true;
			selected = true;
		}

		input.addEventListener("change", function (e) {
			if (this.checked) {
				localStorage.setItem("media.profile", this.value);
			}
		});
	}

	if (!selected) {
		mediaProfiles.item(0).checked = true;
		localStorage.setItem("media.profile", mediaProfiles.item(0).value);
	}
}

function initMediaRecorder(stream) {
	mediaRecorder = new MediaRecorder(stream);
	mediaRecorder.onstop = function (e) {
		const soundClips = document.querySelector('#sound-clips');
		const clipContainer = document.createElement('article');
		const audio = document.createElement('audio');
		const playButton = document.createElement('button');
		const deleteButton = document.createElement('button');

		const progress = document.createElement('div');
		progress.classList.add('progress', 'mx-2', 'w-100');
		progress.style.height = "10px";

		const progressBar = document.createElement('div');
		progressBar.classList.add('progress-bar');
		progressBar.setAttribute("role", "progressbar");
		progressBar.setAttribute("aria-valuenow", "0");
		progressBar.setAttribute("aria-valuemin", "0");
		progressBar.setAttribute("aria-valuemax", "100");

		progress.appendChild(progressBar);

		audio.controls = false;

		playButton.innerHTML = '<i class="bi bi-play-fill"></i>';
		playButton.className = 'btn btn-outline-success btn-block';

		deleteButton.innerHTML = '<i class="bi bi-trash"></i>';
		deleteButton.className = 'btn btn-outline-danger btn-block';

		clipContainer.classList.add('d-flex', 'flex-row', 'bd-highlight', 'py-1');
		clipContainer.style.alignItems = 'center';

		clipContainer.appendChild(audio);
		clipContainer.appendChild(playButton);
		clipContainer.appendChild(progress);
		clipContainer.appendChild(deleteButton);
		soundClips.appendChild(clipContainer);

		const blob = new Blob(chunks, { 'type': 'audio/ogg; codecs=opus' });
		chunks = [];

		audio.addEventListener("play", () => {
			window.setAudioSink(audio, speakerSelect.value);
		});
		audio.addEventListener("timeupdate", () => {
			const value = audio.currentTime / audio.duration * 100;

			// progressBar.setAttribute("aria-valuenow", value);
			progressBar.style.width = value + "%";
		});
		audio.addEventListener("ended", () => {
			playButton.innerHTML = '<i class="bi bi-play-fill"></i>';
		});
		audio.src = window.URL.createObjectURL(blob);

		playButton.onclick = function (e) {
			audio.paused ? audio.play() : audio.pause();

			if (audio.paused) {
				playButton.innerHTML = '<i class="bi bi-play-fill"></i>';
			}
			else {
				playButton.innerHTML = '<i class="bi bi-pause-fill"></i>';
			}
		}
		deleteButton.onclick = function (e) {
			audio.pause();
			soundClips.removeChild(clipContainer);
		}
	}
	mediaRecorder.ondataavailable = function (e) {
		chunks.push(e.data);
	}
}

function initTabs() {
	const devicesTab = document.querySelector("#devicesTab");
	devicesTab.addEventListener("shown.bs.tab", function () {
		initDevices();
	});
	devicesTab.addEventListener("hidden.bs.tab", function () {
		if (deviceStream) {
			window.stopMediaTracks(deviceStream);

			deviceStream = null;
		}
		if (audioInputStream) {
			window.stopMediaTracks(audioInputStream);
		}
		if (videoInputStream) {
			window.stopMediaTracks(videoInputStream);
		}
		if (mediaRecorder) {
			try {
				mediaRecorder.stop();
			}
			catch {}

			const recordStart = document.getElementById("record-start");
			const recordStop = document.getElementById("record-stop");

			recordStart.style.display = "";
			recordStop.style.display = "none";
		}
	});
}

function fillForm(devices, stream, constraints) {
	deviceStream = stream;

	const recordStart = document.getElementById("record-start");
	const recordStop = document.getElementById("record-stop");

	recordStart.onclick = function () {
		mediaRecorder.start();

		recordStart.style.display = "none";
		recordStop.style.display = "";
	}

	recordStop.onclick = function () {
		mediaRecorder.stop();

		recordStart.style.display = "";
		recordStop.style.display = "none";
	}

	const audioInputs = devices.filter(device => device.kind === "audioinput");
	const audioOutputs = devices.filter(device => device.kind === "audiooutput");
	const videoInputs = devices.filter(device => device.kind === "videoinput");

	const cameraBlockedAlert = document.getElementById("cameraBlockedAlert");
	const cameraSelect = document.getElementById("cameraSelect");
	const microphoneSelect = document.getElementById("microphoneSelect");
	const speakerSelect = document.getElementById("speakerSelect");
	const meterCanvas = document.getElementById("meter");

	function saveDevices() {
		const form = document.getElementById("deviceSelectForm");
		const data = new FormData(form);
		const devices = Object.fromEntries(data.entries());

		window.saveDeviceChoice(devices);
	};

	const audioInputContainer = document.getElementById("settingsAudioInputContainer");

	const video = document.getElementById("cameraPreview");
	video.srcObject = stream;

	initMediaRecorder(stream);

	const onAudioInputDeviceChange = () => {
		window.stopAudioTracks(video.srcObject);

		const audioSource = microphoneSelect.value;
		const audioConstraints = {
			audio: {
				deviceId: audioSource ? { exact: audioSource } : undefined
			}
		};

		navigator.mediaDevices.getUserMedia(audioConstraints)
			.then(audioStream => {
				audioInputStream = audioStream;

				initMediaRecorder(audioStream);

				window.getAudioLevel(audioStream.getAudioTracks()[0], meterCanvas);

				saveDevices();
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
			});
	};
	const onAudioOutputDeviceChange = () => {
		window.setAudioSink(video, speakerSelect.value);

		saveDevices();
	};
	const onVideoDeviceChange = () => {
		window.stopVideoTracks(video.srcObject);

		const videoSource = cameraSelect.value;
		const videoConstraints = {};

		if (videoSource === "none") {
			video.style.display = "none";
			saveDevices();
			return;
		}

		videoConstraints.video = {
			deviceId: videoSource ? { exact: videoSource } : undefined,
			width: 1280,
			height: 720,
			facingMode: "user"
		};

		navigator.mediaDevices.getUserMedia(videoConstraints)
			.then(videoStream => {
				const newStream = new MediaStream();

				video.srcObject.getAudioTracks().forEach(track => newStream.addTrack(track));
				videoStream.getVideoTracks().forEach(track => newStream.addTrack(track));

				videoInputStream = newStream;

				video.srcObject = newStream;
				video.style.display = "block";

				cameraBlockedAlert.classList.add("d-none");

				saveDevices();
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotReadableError") {
					cameraBlockedAlert.classList.remove("d-none");
				}
				else if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
			});
	};

	cameraSelect.length = 1;
	microphoneSelect.length = 0;
	speakerSelect.length = 0;
	cameraSelect.onchange = onVideoDeviceChange;
	microphoneSelect.onchange = onAudioInputDeviceChange;
	speakerSelect.onchange = onAudioOutputDeviceChange;

	const resetButton = document.getElementById("resetDeviceSelection");
	resetButton.addEventListener("click", () => {
		cameraSelect.selectedIndex = 0;
		microphoneSelect.selectedIndex = 0;
		speakerSelect.selectedIndex = 0;

		window.clearDeviceChoice();

		onAudioInputDeviceChange();
		onAudioOutputDeviceChange();
		onVideoDeviceChange();
	});

	const audioSink = localStorage.getItem("audiooutput");

	for (const device of audioInputs) {
		const index = microphoneSelect.options.length;
		const selected = constraints.audio.deviceId?.exact == device.deviceId;

		microphoneSelect.options[index] = new Option(window.removeHwId(device.label), device.deviceId, false, selected);
	}

	for (const device of audioOutputs) {
		const index = speakerSelect.options.length;
		const selected = audioSink == device.deviceId;

		speakerSelect.options[index] = new Option(window.removeHwId(device.label), device.deviceId, false, selected);
	}

	for (const device of videoInputs) {
		const index = cameraSelect.options.length;
		const selected = constraints.video?.deviceId?.exact == device.deviceId;

		cameraSelect.options[index] = new Option(window.removeHwId(device.label), device.deviceId, false, selected);
	}

	if (cameraSelect.value === "none") {
		window.stopVideoTracks(stream);
	}

	audioInputContainer.style.display = audioOutputs.length ? "block" : "none";
	video.style.display = (cameraSelect.value === "none") ? "none" : "block";

	window.getAudioLevel(stream.getAudioTracks()[0], meterCanvas);
}

function initDevices() {
	window.enumerateDevices(true, true)
	.then(result => {
		fillForm(result.devices, result.stream, result.constraints);
	})
	.catch(error => {
		console.error(error);

		if (error.name == "NotReadableError") {
			const cameraBlockedAlert = document.getElementById("cameraBlockedAlert");
			cameraBlockedAlert.classList.toggle("d-none");

			window.enumerateDevices(false, true)
				.then(result => {
					fillForm(result.devices, result.stream, result.constraints);
				})
				.catch(error => {
					console.error(error);

					showDevicePermissionDeniedModal();
				});
		}
		else {
			window.enumerateDevices(false, false)
				.then(result => {
					fillForm(result.devices, result.stream, result.constraints);
				})
				.catch(error => {
					console.error(error);

					showDevicePermissionDeniedModal();
				});
		}
	});
}

function showDevicePermissionDeniedModal() {
	const deviceContainer = document.getElementById("devices");
	deviceContainer.classList.add("disabled-content");

	const deviceModalElement = document.getElementById("deviceModalPermission");
	const deviceModal = bootstrap.Modal.getOrCreateInstance(deviceModalElement, {
		backdrop: "static",
		keyboard: false
	});

	const hiddenHandler = () => {
		deviceModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
		deviceModal.dispose();
	};

	deviceModalElement.addEventListener("hidden.bs.modal", hiddenHandler);

	deviceModal.show();
}

initTabs();
initMediaProfile();
