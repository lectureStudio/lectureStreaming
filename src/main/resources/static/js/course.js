class Course {

	constructor() {
		this.loadingContainer = null;
		this.connectionInfoContainer = null;
		this.unavailableContainer = null;
		this.contentContainer = null;
		this.messengerContainer = null;
		this.messengerElement = null;
		this.quizContainer = null;
		this.quizElement = null;
		this.quizModal = null;
		this.player = null;
		this.userId = null;
		this.courseId = null;
		this.speechRequestId = null;
		this.speechModal = null;
		this.startTime = null;
		this.dict = null;
		this.devicesSelected = false;
		this.courseRecordedModal = null;
	}

	init(userId, courseId, startTime, isRecorded, dict) {
		this.userId = userId;
		this.courseId = courseId;
		this.startTime = startTime;
		this.dict = dict;
		this.loadingContainer = document.getElementById("course-loading");
		this.connectionInfoContainer = document.getElementById("connection-info");
		this.unavailableContainer = document.getElementById("course-unavailable");
		this.contentContainer = document.getElementById("course-content");
		this.messengerContainer = document.getElementById("messenger-content");
		this.quizContainer = document.getElementById("quiz-content");

		const courseRecordeElement = document.getElementById("recordedModal");
		this.courseRecordedModal = bootstrap.Modal.getOrCreateInstance(courseRecordeElement, {
			backdrop: "static",
			keyboard: false
		});

		window.dict = dict;

		window.portalApp.addOnCourseState((event) => {
			if (event.courseId !== this.courseId) {
				return;
			}

			if (event.started) {
				this.startTime = event.createdTimestamp;

				if (this.messengerElement) {
					this.messengerContainer.removeChild(this.messengerElement);
				}
				if (this.quizElement) {
					removeAllChildNodes(this.quizContainer);
					this.quizContainer.classList.add("d-none");
	
					const submitButton = this.quizElement.querySelector("#quizSubmit");
					submitButton.classList.add("d-none");
				}

				this.initPlayer();
			}
			else {
				if (this.quizModal) {
					this.quizModal.hide();
				}

				if (!this.player) {
					this.connectionInfoVisible(false)
					this.unavailableVisible(true);
				}
				else {
					this.onPlayerConnectedState(false);
				}

				this.player = null;
			}
		});
		window.portalApp.addOnCourseRecordedState((event) => {
			if (event.courseId !== this.courseId) {
				return;
			}

			if (event.started) {
				this.courseRecordedModal.show();
			}
			else {
				this.courseRecordedModal.hide();
			}
		});
		window.portalApp.addOnSpeechState((event) => {
			if (!this.player) {
				return;
			}

			if (event.requestId >> 3 === this.speechRequestId >> 3) {
				if (event.accepted) {
					this.speechAccepted();
				}
				else {
					this.speechRequestId = null;

					this.player.stopSpeech();

					if (this.speechModal) {
						this.speechModal.hide();
					}

					this.showToast("toast-warn", "course.speech.request.rejected");
				}
			}
		});
		window.portalApp.addOnMessengerState((event) => {
			if (event.courseId !== this.courseId) {
				return;
			}

			if (event.started) {
				this.loadMessenger();
			}
			else {
				if (this.player) {
					this.player.setContainerA(null);
				}
				else {
					removeAllChildNodes(this.messengerContainer);
					this.messengerContainer.classList.add("d-none");
				}

				this.messengerElement = null;
				this.unavailableVisible(true);
			}
		});
		window.portalApp.addOnQuizState((event) => {
			if (event.courseId !== this.courseId) {
				return;
			}

			if (event.started) {
				this.loadQuiz();
			}
			else {
				if (this.quizModal) {
					this.quizModal.hide();
				}

				if (this.player) {
					this.player.setQuizActive(false);
				}

				removeAllChildNodes(this.quizContainer);
				this.quizContainer.classList.add("d-none");

				this.quizElement = null;
				this.unavailableVisible(true);
			}
		});
		window.addEventListener("unload", () => {
			this.cancelSpeech();
		});
		window.onbeforeunload = () => {
			this.cancelSpeech();
			return null;
		}

		if (isRecorded) {
			this.courseRecordedModal.show();
		}
	}

	showFeatures(show) {
		const featureContainer = document.getElementById("course-feature-container");

		if (show) {
			featureContainer.classList.remove("d-none");
		}
		else {
			featureContainer.classList.add("d-none");
		}
	}

	initPlayer() {
		const mediaProfile = localStorage.getItem("media.profile");

		if (mediaProfile === "classroom") {
			this.loadingVisible(false);

			this.showFeatures(true);
			return;
		}

		this.loadingVisible(true);
		this.unavailableVisible(false);
		this.connectionInfoVisible(false);

		const deviceConstraints = {
			audioInput: localStorage.getItem("audioinput"),
			audioOutput: localStorage.getItem("audiooutput"),
			videoInput: localStorage.getItem("videoinput")
		};

		this.player = new lect.LecturePlayer();
		this.player.setDeviceConstraints(deviceConstraints);
		this.player.setContainer(this.contentContainer);
		this.player.setUserId(this.userId);
		this.player.setRoomId(this.courseId);
		this.player.setStartTime(this.startTime);
		this.player.setOnError(this.onPlayerError.bind(this));
		this.player.setOnConnectedState(this.onPlayerConnectedState.bind(this));
		this.player.setOnConnectedSpeechState(this.onSpeechConnectedState.bind(this));
		this.player.setOnSettings(this.onPlayerSettings.bind(this));
		this.player.setOnRaiseHand(this.onRaiseHand.bind(this));
		this.player.setOnShowQuiz(this.onShowQuiz.bind(this));
		this.player.start()
			.catch(this.onPlayerError.bind(this));
	}

	onPlayerError(error) {
		console.log(error);

		this.player = null;

		this.detachFeaturesFromPlayer();

		this.playerVisible(false);
		this.loadingVisible(false);
		this.unavailableVisible(false);
		this.connectionInfoVisible(true);
	}

	detachFeaturesFromPlayer() {
		if (this.messengerElement) {
			this.messengerContainer.appendChild(this.messengerElement);
			this.messengerContainer.classList.remove("d-none");
		}
		if (this.quizElement) {
			const submitButton = this.quizElement.querySelector("#quizSubmit");
			submitButton.classList.remove("d-none");

			this.quizContainer.appendChild(this.quizElement);
			this.quizContainer.classList.remove("d-none");
		}
	}

	onPlayerConnectedState(connected) {
		if (connected) {
			this.showFeatures(false);

			const playerContainer = document.getElementById("playerContainer");
			const speechDeviceModal = document.getElementById("speechDeviceModal");
			const speechAcceptedModal = document.getElementById("speechAcceptedModal");
			const deviceModal = document.getElementById("deviceModal");
			const deviceModalInit = document.getElementById("deviceModalInit");
			const deviceModalPermission = document.getElementById("deviceModalPermission");
			const quizModal = document.getElementById("quizModal");
			const recordedModal = document.getElementById("recordedModal");
			const toastContainer = document.getElementById("toast-container");

			playerContainer.appendChild(speechDeviceModal);
			playerContainer.appendChild(speechAcceptedModal);
			playerContainer.appendChild(deviceModal);
			playerContainer.appendChild(deviceModalInit);
			playerContainer.appendChild(deviceModalPermission);
			playerContainer.appendChild(quizModal);
			playerContainer.appendChild(recordedModal);
			playerContainer.appendChild(toastContainer);

			if (this.messengerElement) {
				this.player.setContainerA(this.messengerElement);
			}
			if (this.quizElement) {
				this.player.setQuizActive(true);
			}

			this.loadingVisible(false);
			this.connectionInfoVisible(false);
			this.unavailableVisible(false);
			this.playerVisible(true);
		}
		else {
			this.detachFeaturesFromPlayer();

			this.player = null;

			this.playerVisible(false);
			this.connectionInfoVisible(false);
			this.unavailableVisible(true);
		}
	}

	onPlayerSettings() {
		const modalElement = document.getElementById("deviceModal");

		window.enumerateDevices(true, true)
			.then(result => {
				this.showDeviceChooserModal(modalElement, result.devices, result.stream, result.constraints, false);
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotReadableError") {
					window.enumerateDevices(false, true)
						.then(result => {
							this.showDeviceChooserModal(modalElement, result.devices, result.stream, result.constraints, true);
						})
						.catch(error => {
							console.error(error);
						});
				}
				else if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
				else {
					window.enumerateDevices(false, false)
						.then(result => {
							this.showDeviceChooserModal(modalElement, result.devices, result.stream, result.constraints, false);
						})
						.catch(error => {
							console.error(error);
						});
				}
			});
	}

	onSpeechConnectedState(connected) {
		window.dispatchEvent(new Event('resize'));

		if (connected) {
			this.showToast("toast-success", "course.speech.request.speak");
		}
		else {
			this.showToast("toast-warn", "course.speech.request.ended");
		}
	}

	onRaiseHand(raised) {
		if (raised) {
			this.initSpeech();
		}
		else {
			this.cancelSpeech();

			if (this.player) {
				this.player.stopSpeech();
			}
		}
	}

	onShowQuiz(show) {
		if (show) {
			this.initQuizModal();
		}
	}

	initSpeech() {
		if (!this.devicesSelected) {
			this.getUserDevices();
		}
		else {
			this.sendSpeechRequest();
		}
	}

	cancelSpeech() {
		if (!this.speechRequestId) {
			if (this.player) {
				this.player.setRaiseHand(false);
			}
			return;
		}

		fetch("/course/speech/" + this.courseId + "/" + this.speechRequestId, {
			method: "DELETE"
		})
		.then(response => {
			this.speechRequestId = null;
			this.player.setRaiseHand(false);
		})
		.catch(error => console.error(error));
	}

	sendSpeechRequest() {
		fetch("/course/speech/" + this.courseId, {
			method: "POST"
		})
		.then(response => {
			return response.text();
		})
		.then(requestId => {
			this.speechRequestId = requestId;
		})
		.catch(error => console.error(error));
	}

	loadMessenger() {
		fetch("/course/messenger/" + this.courseId, {
			method: "GET",
		})
		.then((response) => {
			return response.text();
		})
		.then(html => {
			if (html) {
				const doc = new DOMParser().parseFromString(html, "text/html");

				this.messengerElement = document.createElement("div");

				for (const child of doc.body.children) {
					this.messengerElement.appendChild(child);
				}

				if (this.player) {
					this.player.setContainerA(this.messengerElement);
				}
				else {
					this.messengerContainer.appendChild(this.messengerElement);
					this.messengerContainer.classList.remove("d-none");
				}

				this.initMessenger();
			}
		})
		.catch(error => console.error(error));
	}

	initMessenger() {
		const messageForm = this.messengerElement.querySelector("#course-message-form");

		if (!messageForm) {
			return;
		}

		this.unavailableVisible(false);

		messageForm.addEventListener("submit", (event) => {
			// Disable default action.
			event.preventDefault();

			const submitButton = messageForm.querySelector("#messageSubmit");
			submitButton.disabled = true;

			const data = new FormData(event.target);
			const value = Object.fromEntries(data.entries());

			fetch(messageForm.getAttribute("action"), {
				method: "POST",
				body: JSON.stringify(value),
				headers: {
					"Content-Type": "application/json"
				}
			})
			.then(response => {
				const toastId = (response.status === 200) ? "toast-success" : "toast-warn";
				const toastMessage = (response.status === 200) ? "course.feature.message.sent" : "course.feature.message.send.error";

				this.showToast(toastId, toastMessage);

				messageForm.reset();
				submitButton.disabled = false;
			})
			.catch(error => console.error(error));
		});
	}

	loadQuiz() {
		fetch("/course/quiz/" + this.courseId, {
			method: "GET",
		})
		.then((response) => {
			return response.text();
		})
		.then(html => {
			if (html) {
				const doc = new DOMParser().parseFromString(html, "text/html");

				this.quizElement = document.createElement("div");
				this.quizElement.innerHTML = doc.body.innerHTML;

				this.initQuiz();
			}
		})
		.catch(error => console.error(error));
	}

	initQuiz() {
		const quizForm = this.quizElement.querySelector("#course-quiz-form");

		if (!quizForm) {
			return;
		}

		this.unavailableVisible(false);

		quizForm.addEventListener("submit", (event) => {
			// Disable default action.
			event.preventDefault();

			const submitButton = quizForm.querySelector("#quizSubmit");
			submitButton.disabled = true;

			const data = new FormData(event.target);
			const value = Object.fromEntries(data.entries());
			value.options = data.getAll("options");

			fetch(quizForm.getAttribute("action"), {
				method: "POST",
				body: JSON.stringify(value),
				headers: {
					"Content-Type": "application/json"
				}
			})
			.then(response => {
				quizForm.reset();
				submitButton.disabled = false;

				return response.json();
			})
			.then(serviceResponse => {
				const statusCode = serviceResponse.statusCode;
				const toastMessage = serviceResponse.statusMessage;
				const toastId = (statusCode === 0) ? "toast-success" : "toast-warn";

				this.showToast(toastId, toastMessage);
			})
			.catch(error => console.error(error));
		});

		if (this.player) {
			const submitButton = quizForm.querySelector("#quizSubmit");
			submitButton.classList.add("d-none");

			this.player.setQuizActive(true);
		}
		else {
			this.quizContainer.appendChild(this.quizElement);
			this.quizContainer.classList.remove("d-none");
		}
	}

	initQuizModal() {
		const quizModalElement = document.getElementById("quizModal");
		const quizModalContent = document.getElementById("quizModalContent");

		this.quizModal = bootstrap.Modal.getOrCreateInstance(quizModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const hiddenHandler = () => {
			quizModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);

			if (this.player) {
				this.player.setShowQuiz(false);
			}

			removeAllChildNodes(quizModalContent);

			this.quizModal.dispose();
			this.quizModal = null;
		};

		quizModalElement.addEventListener("hidden.bs.modal", hiddenHandler);
		quizModalContent.appendChild(this.quizElement);

		this.quizModal.show();
	}

	speechAccepted() {
		const speechConstraints = {
			audioDeviceId: localStorage.getItem("audioinput"),
			videoDeviceId: localStorage.getItem("videoinput")
		};

		const constraints = {
			video: {
				deviceId: speechConstraints.videoDeviceId ? { exact: speechConstraints.videoDeviceId } : undefined
			}
		};

		navigator.mediaDevices.getUserMedia(constraints)
			.then(stream => {
				this.showSpeechAcceptedModal(stream, speechConstraints, false);
			})
			.catch(error => {
				console.error(error.name);

				speechConstraints.videoDeviceId = undefined;

				this.showSpeechAcceptedModal(null, speechConstraints, true);
			});
	}

	showSpeechAcceptedModal(stream, speechConstraints, camReadableError) {
		const speechModalElement = document.getElementById("speechAcceptedModal");
		const cameraBlockedAlert = document.getElementById("cameraBlockedModalAlert");
		const startButton = document.getElementById("speechRequestStart");
		const cancelButton = document.getElementById("speechRequestCancel");

		this.speechModal = bootstrap.Modal.getOrCreateInstance(speechModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const cancelHandler = () => {
			this.cancelSpeech();
		};
		const startHandler = () => {
			this.player.startSpeech(speechConstraints);

			this.speechModal.hide();
		};
		const hiddenHandler = () => {
			speechModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			cancelButton.removeEventListener("click", cancelHandler);
			startButton.removeEventListener("click", startHandler);
			this.speechModal.dispose();

			window.stopMediaTracks(stream);
		};

		cancelButton.addEventListener("click", cancelHandler);
		startButton.addEventListener("click", startHandler);
		speechModalElement.addEventListener("hidden.bs.modal", hiddenHandler);

		if (camReadableError) {
			cameraBlockedAlert.classList.remove("d-none");
		}
		else {
			cameraBlockedAlert.classList.add("d-none");
		}

		if (this.speechRequestId) {
			this.speechModal.show();
		}
		else {
			// Speech has been aborted by the remote peer.
			window.stopMediaTracks(stream);

			this.speechModal = null;
		}
	}

	showDeviceInitModal() {
		const nextButton = document.getElementById("nextDeviceSelection");
		const cancelButton = document.getElementById("deviceInitCancel");

		const deviceModalElement = document.getElementById("deviceModalInit");
		const deviceModal = bootstrap.Modal.getOrCreateInstance(deviceModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const cancelHandler = () => {
			this.cancelSpeech();
		};
		const nextHandler = () => {
			const form = document.getElementById("deviceSelectInitForm");
			const data = new FormData(form);
			const options = Object.fromEntries(data.entries());

			this.getUserDevices(options);

			deviceModal.hide();
		};
		const hiddenHandler = () => {
			deviceModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			cancelButton.removeEventListener("click", cancelHandler);
			nextButton.removeEventListener("click", nextHandler);
			deviceModal.dispose();
		};

		cancelButton.addEventListener("click", cancelHandler);
		nextButton.addEventListener("click", nextHandler);
		deviceModalElement.addEventListener("hidden.bs.modal", hiddenHandler);

		deviceModal.show();
	}

	showDevicePermissionDeniedModal() {
		const deviceModalElement = document.getElementById("deviceModalPermission");
		const deviceModal = bootstrap.Modal.getOrCreateInstance(deviceModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const hiddenHandler = () => {
			deviceModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			deviceModal.dispose();

			this.cancelSpeech();
		};

		deviceModalElement.addEventListener("hidden.bs.modal", hiddenHandler);

		deviceModal.show();
	}

	showDeviceChooserModal(deviceModalElement, devices, stream, constraints, camBlocked) {
		const audioInputs = devices.filter(device => device.kind === "audioinput");
		const audioOutputs = devices.filter(device => device.kind === "audiooutput");
		const videoInputs = devices.filter(device => device.kind === "videoinput");

		const settingsElement = document.importNode(document.querySelector("#deviceSettings").content, true);

		const cameraBlockedAlert = settingsElement.querySelector("#cameraBlockedModalAlert");
		const cameraSelect = settingsElement.querySelector("#cameraSelect");
		const microphoneSelect = settingsElement.querySelector("#microphoneSelect");
		const speakerSelect = settingsElement.querySelector("#speakerSelect");
		const audioInputContainer = settingsElement.querySelector("#settingsAudioInputContainer");
		const meterCanvas = settingsElement.querySelector("#meter");
		const deviceForm = settingsElement.querySelector("#deviceSelectForm");
		const saveButton = deviceModalElement.querySelector("#saveDeviceSelection");
		const cancelButton = deviceModalElement.querySelector("#deviceSaveCancel");
		
		const video = settingsElement.querySelector("#cameraPreview");
		video.srcObject = stream;
		video.muted = true;

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
				cameraBlockedAlert.classList.add("d-none");
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
		cameraSelect.options.length = 1;

		microphoneSelect.onchange = onAudioInputDeviceChange;
		microphoneSelect.options.length = 0;

		speakerSelect.onchange = onAudioOutputDeviceChange;
		speakerSelect.options.length = 0;

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

		video.style.display = (cameraSelect.value === "none") ? "none" : "block";

		const deviceModalBody = deviceModalElement.querySelector(".modal-body");
		deviceModalBody.appendChild(settingsElement);

		const deviceModal = bootstrap.Modal.getOrCreateInstance(deviceModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const cancelHandler = () => {
			
		};
		const saveHandler = () => {
			const data = new FormData(deviceForm);
			const devices = Object.fromEntries(data.entries());

			window.saveDeviceChoice(devices);

			this.devicesSelected = true;

			deviceModal.hide();
		};
		const hiddenHandler = () => {
			deviceModalBody.innerHTML = "";
			deviceModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			cancelButton.removeEventListener("click", cancelHandler);
			saveButton.removeEventListener("click", saveHandler);
			deviceModal.dispose();

			window.stopMediaTracks(stream);

			video.srcObject = null;

			const settingsButton = document.querySelector("#controlContainer #settingsButton");
			settingsButton.disabled = false;
		};

		cancelButton.addEventListener("click", cancelHandler);
		saveButton.addEventListener("click", saveHandler);
		deviceModalElement.addEventListener("hidden.bs.modal", hiddenHandler);
		deviceModalElement.addEventListener("shown.bs.modal", () => {
			const audioTrack = stream.getAudioTracks()[0];

			window.getAudioLevel(audioTrack, meterCanvas);
		});

		audioInputContainer.style.display = audioOutputs.length ? "block" : "none";

		if (camBlocked && constraints.video) {
			cameraBlockedAlert.classList.remove("d-none");
		}
		else {
			cameraBlockedAlert.classList.add("d-none");
		}

		deviceModal.show();
	}

	showToast(toastId, messageId) {
		const toastElement = document.getElementById(toastId);
		const toastBody = toastElement.getElementsByClassName("toast-body")[0];
		toastBody.innerHTML = this.dict[messageId];

		const toast = new bootstrap.Toast(toastElement);
		toast.show();
	}

	showSpeechDeviceChooserModal(devices, stream, constraints, camBlocked) {
		const audioInputs = devices.filter(device => device.kind === "audioinput");
		const audioOutputs = devices.filter(device => device.kind === "audiooutput");
		const videoInputs = devices.filter(device => device.kind === "videoinput");

		const settingsElement = document.importNode(document.querySelector("#deviceSettings").content, true);

		const cameraBlockedAlert = settingsElement.querySelector("#cameraBlockedModalAlert");
		const cameraSelect = settingsElement.querySelector("#cameraSelect");
		const microphoneSelect = settingsElement.querySelector("#microphoneSelect");
		const speakerSelect = settingsElement.querySelector("#speakerSelect");
		const audioInputContainer = settingsElement.querySelector("#settingsAudioInputContainer");
		const meterCanvas = settingsElement.querySelector("#meter");
		const deviceForm = settingsElement.querySelector("#deviceSelectForm");
		const saveButton = document.getElementById("saveDeviceSelection");
		const cancelButton = document.getElementById("deviceSaveCancel");
		
		const video = settingsElement.querySelector("#cameraPreview");
		video.srcObject = stream;
		video.muted = true;

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
				cameraBlockedAlert.classList.add("d-none");
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
		cameraSelect.options.length = 1;

		microphoneSelect.onchange = onAudioInputDeviceChange;
		microphoneSelect.options.length = 0;

		speakerSelect.onchange = onAudioOutputDeviceChange;
		speakerSelect.options.length = 0;

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

		video.style.display = (cameraSelect.value === "none") ? "none" : "block";

		const deviceModalElement = document.getElementById("speechDeviceModal");
		const deviceModalBody = deviceModalElement.querySelector(".modal-body");
		deviceModalBody.appendChild(settingsElement);

		const deviceModal = bootstrap.Modal.getOrCreateInstance(deviceModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const cancelHandler = () => {
			this.cancelSpeech();
		};
		const saveHandler = () => {
			const data = new FormData(deviceForm);
			const devices = Object.fromEntries(data.entries());

			window.saveDeviceChoice(devices);

			this.devicesSelected = true;
			this.sendSpeechRequest();

			deviceModal.hide();
		};
		const hiddenHandler = () => {
			deviceModalBody.innerHTML = "";
			deviceModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			cancelButton.removeEventListener("click", cancelHandler);
			saveButton.removeEventListener("click", saveHandler);
			deviceModal.dispose();

			window.stopMediaTracks(stream);

			video.srcObject = null;
		};

		cancelButton.addEventListener("click", cancelHandler);
		saveButton.addEventListener("click", saveHandler);
		deviceModalElement.addEventListener("hidden.bs.modal", hiddenHandler);
		deviceModalElement.addEventListener("shown.bs.modal", () => {
			const audioTrack = stream.getAudioTracks()[0];

			window.getAudioLevel(audioTrack, meterCanvas);
		});

		audioInputContainer.style.display = audioOutputs.length ? "block" : "none";

		if (camBlocked && constraints.video) {
			cameraBlockedAlert.classList.remove("d-none");
		}
		else {
			cameraBlockedAlert.classList.add("d-none");
		}

		deviceModal.show();
	}

	getUserDevices() {
		window.enumerateDevices(true, true)
			.then(result => {
				this.showSpeechDeviceChooserModal(result.devices, result.stream, result.constraints, false);
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotReadableError") {
					window.enumerateDevices(false, true)
						.then(result => {
							this.showSpeechDeviceChooserModal(result.devices, result.stream, result.constraints, true);
						})
						.catch(error => {
							console.error(error);
						});
				}
				else if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
				else {
					window.enumerateDevices(false, false)
						.then(result => {
							this.showSpeechDeviceChooserModal(result.devices, result.stream, result.constraints, false);
						})
						.catch(error => {
							console.error(error);
						});
				}
			});
	}

	loadingVisible(visible) {
		this.elementVisible(this.loadingContainer, visible);
	}

	connectionInfoVisible(visible) {
		this.elementVisible(this.connectionInfoContainer, visible);
	}

	unavailableVisible(visible) {
		this.elementVisible(this.unavailableContainer, visible & !(this.player || this.messengerElement || this.quizElement || !this.connectionInfoContainer.classList.contains("d-none")));
	}

	playerVisible(visible) {
		if (visible) {
			this.contentContainer.classList.remove("invisible");
			this.contentContainer.classList.add("flex-grow-1");
		}
		else {
			this.contentContainer.classList.add("invisible");
			this.contentContainer.classList.remove("flex-grow-1");

			removeAllChildNodes(this.contentContainer);
		}
	}

	elementVisible(element, visible) {
		if (visible) {
			element.classList.remove("d-none");
		}
		else {
			element.classList.add("d-none");
		}
	}
}

window.course = new Course();