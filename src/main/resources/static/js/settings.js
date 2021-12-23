function fillForm(devices, stream, constraints) {
	const audioInputs = devices.filter(device => device.kind === "audioinput");
	const audioOutputs = devices.filter(device => device.kind === "audiooutput");
	const videoInputs = devices.filter(device => device.kind === "videoinput");

	const cameraBlockedAlert = document.getElementById("cameraBlockedAlert");
	const cameraSelect = document.getElementById("cameraSelect");
	const microphoneSelect = document.getElementById("microphoneSelect");
	const speakerSelect = document.getElementById("speakerSelect");
	const meterCanvas = document.getElementById("meter");

	const saveButton = document.getElementById("saveDeviceSelection");
	saveButton.addEventListener("click", () => {
		const form = document.getElementById("deviceSelectForm");
		const data = new FormData(form);
		const devices = Object.fromEntries(data.entries());

		window.saveDeviceChoice(devices);
	});

	const audioInputContainer = document.getElementById("settingsAudioInputContainer");

	const video = document.getElementById("cameraPreview");
	video.srcObject = stream;

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
				audioStream.getAudioTracks().forEach(track => video.srcObject.addTrack(track));

				window.getAudioLevel(audioStream.getAudioTracks()[0], meterCanvas);
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
			});
	};
	const onAudioOutputDeviceChange = () => {
		if (!('sinkId' in HTMLMediaElement.prototype)) {
			return;
		}

		const audioSink = speakerSelect.value;

		video.setSinkId(audioSink)
			.catch(error => {
				console.error(error);
			});
	};
	const onVideoDeviceChange = () => {
		window.stopVideoTracks(video.srcObject);

		const videoSource = cameraSelect.value;
		const videoConstraints = {};

		if (videoSource === "none") {
			video.style.display = "none";
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

				video.srcObject = newStream;
				video.style.display = "block";

				cameraBlockedAlert.classList.add("d-none");
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
				});
		}
		else {
			window.enumerateDevices(false, false)
				.then(result => {
					fillForm(result.devices, result.stream, result.constraints);
				})
				.catch(error => {
					console.error(error);
				});
		}
	});