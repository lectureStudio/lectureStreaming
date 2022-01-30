function addGainToStream(inputStream, gain){
	const audioContext = new AudioContext()
	const gainNode = audioContext.createGain(); 
	const audioSource = audioContext.createMediaStreamSource(inputStream);
	const audioDestination = audioContext.createMediaStreamDestination();
	audioSource.connect(gainNode);
	gainNode.connect(audioDestination);
	gain = (Math.pow(10, gain) - 1) / 10;
	gainNode.gain.value = gain;
	return audioDestination.stream;
}

let orginialStream = null;

function addGainToOriginalStream(gain){
	return addGainToStream(orginialStream, gain / 100);
}

function fillForm(devices, inputStream, constraints) {
	const audioInputs = devices.filter(device => device.kind === "audioinput");
	const videoInputs = devices.filter(device => device.kind === "videoinput");

	const cameraBlockedAlert = document.getElementById("cameraBlockedAlert");
	const cameraSelect = document.getElementById("cameraSelect");
	const microphoneSelect = document.getElementById("microphoneSelect");
	const meterCanvas = document.getElementById("meter");
	const volume = document.getElementById("microphoneVolume");

	const saveButton = document.getElementById("saveDeviceSelection");
	saveButton.addEventListener("click", () => {
		const form = document.getElementById("deviceSelectForm");
		const data = new FormData(form);
		const devices = Object.fromEntries(data.entries());

		window.saveDeviceChoice(devices);
	});

	const video = document.getElementById("cameraPreview");
	orginialStream = inputStream;
	const stream = addGainToStream(inputStream, volume.value / 100);
	video.srcObject = stream;

	const onAudioDeviceChange = () => {
		window.stopAudioTracks(video.srcObject);

		const audioSource = microphoneSelect.value;
		const audioConstraints = {
			audio: {
				deviceId: audioSource ? { exact: audioSource } : undefined
			}
		};

		navigator.mediaDevices.getUserMedia(audioConstraints)
			.then(audioStream => {
				orginialStream = audioStream;
				const gainedStream = addGainToStream(audioStream, volume.value / 100);
				gainedStream.getAudioTracks().forEach(track => video.srcObject.addTrack(track));

				window.getAudioLevel(gainedStream.getAudioTracks()[0], meterCanvas);
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
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

	volume.addEventListener('input', () => {
		onAudioDeviceChange();
	});

	cameraSelect.onchange = onVideoDeviceChange;
	microphoneSelect.onchange = onAudioDeviceChange;

	const resetButton = document.getElementById("resetDeviceSelection");
	resetButton.addEventListener("click", () => {
		cameraSelect.selectedIndex = 0;
		microphoneSelect.selectedIndex = 0;

		window.clearDeviceChoice();

		onAudioDeviceChange();
		onVideoDeviceChange();
	});

	for (const device of audioInputs) {
		const index = microphoneSelect.options.length;
		const selected = constraints.audio.deviceId?.exact == device.deviceId;

		microphoneSelect.options[index] = new Option(window.removeHwId(device.label), device.deviceId, false, selected);
	}

	for (const device of videoInputs) {
		const index = cameraSelect.options.length;
		const selected = constraints.video?.deviceId?.exact == device.deviceId;

		cameraSelect.options[index] = new Option(window.removeHwId(device.label), device.deviceId, false, selected);
	}

	if (cameraSelect.value === "none") {
		window.stopVideoTracks(stream);
	}

	video.style.display = (cameraSelect.value === "none") ? "none" : "block";

	window.getAudioLevel(stream.getAudioTracks()[0], meterCanvas);
}

window.enumerateDevices(true)
	.then(result => {
		fillForm(result.devices, result.stream, result.constraints);
	})
	.catch(error => {
		console.error(error);

		if (error.name == "NotReadableError") {
			const cameraBlockedAlert = document.getElementById("cameraBlockedAlert");
			cameraBlockedAlert.classList.toggle("d-none");

			window.enumerateDevices(false)
				.then(result => {
					fillForm(result.devices, result.stream, result.constraints);
				})
				.catch(error => {
					console.error(error);
				});
		}
	});