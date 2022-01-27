class Course {

	constructor() {
		this.loadingContainer = null;
		this.unavailableContainer = null;
		this.contentContainer = null;
		this.messengerContainer = null;
		this.messengerElement = null;
		this.quizContainer = null;
		this.quizElement = null;
		this.player = null;
		this.userId = null;
		this.courseId = null;
		this.speechRequestId = null;
		this.startTime = null;
		this.dict = null;
		this.stompClient = null;
		this.messengerIsVisible = false;
		this.messengerModal = null;
	}

	init(userId, courseId, startTime, dict) {
		this.userId = userId;
		this.courseId = courseId;
		this.startTime = startTime;
		this.dict = dict;
		this.loadingContainer = document.getElementById("course-loading");
		this.unavailableContainer = document.getElementById("course-unavailable");
		this.contentContainer = document.getElementById("course-content");
		this.messengerContainer = document.getElementById("messenger-content");
		this.quizContainer = document.getElementById("quiz-content");
		this.isPlayerUsedContainer = document.getElementById("player-is-used");


		window.portalApp.addOnCourseState((event) => {
			if (event.started) {
				this.startTime = event.createdTimestamp;

				this.initPlayer();
			}
			else {
				this.player = null;
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
					this.player.stopSpeech();

					this.showToast("toast-warn", "course.speech.request.rejected");
				}
			}
		});
		window.portalApp.addOnMessengerState((event) => {
			if (event.started) {
				this.loadMessenger();
			}
			else {
				this.teardownMessengerConnection();
				if (this.player) {
					this.player.setContainerA(null);
				}
				else {
					removeAllChildNodes(this.messengerContainer);
				}

				this.messengerElement = null;
				this.unavailableVisible(true);
			}
		});
		window.portalApp.addOnQuizState((event) => {
			if (event.started) {
				this.loadQuiz();
			}
			else {
				if (this.player) {
					this.player.setQuizActive(false);
				}

				removeAllChildNodes(this.quizContainer);

				this.quizElement = null;
				this.unavailableVisible(true);
			}
		});
		window.addEventListener('resize', () => {
			this.messengerResize();
		});
		this.isPlayerUsedContainer.addEventListener('change', () => {
			if(this.isPlayerUsed()){
				this.initPlayer();
			}else{
				location.reload(); 
			}
		})
	}

	async establishMessengerConnection() {
		var socket = new SockJS('/messenger');
		this.stompClient = Stomp.over(socket);
		const messengerConnectionWarning = this.messengerElement.querySelector("#messenger-connection-warning");

		this.stompClient.connect({}, async (frame) => {
			if (! messengerConnectionWarning.classList.contains('hidden')) {
				messengerConnectionWarning.classList.add('hidden');
			}
			this.stompClient.subscribe('/topic/chat/' + this.courseId, (message) => {
				this.onChatMessageReceive(this.stompMessageToMessageObject(message));
			});
			await this.reloadMessengerHistory();
			this.setMessengerForm(false);
		},
			(frame) => {
				this.setMessengerForm(true);
				console.log("Lost connection!");
				messengerConnectionWarning.classList.remove('hidden');
		});
	}

	teardownMessengerConnection() {
		this.stompClient.disconnect(() => {
			console.log("Disconnected From STOMP broker");
		})
	}

	initPlayer() {
		if(! this.isPlayerUsed()){
			this.loadingVisible(false);
			this.unavailableVisible(false);
			return;
		}
		this.loadingVisible(true);
		this.unavailableVisible(false);

		this.player = new lect.LecturePlayer();
		this.player.setContainer(this.contentContainer);
		this.player.setUserId(this.userId);
		this.player.setRoomId(this.courseId);
		this.player.setStartTime(this.startTime);
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

		this.playerVisible(false);
		this.loadingVisible(false);
		this.unavailableVisible(true);

		this.player = null;
	}

	onPlayerConnectedState(connected) {
		if (connected) {
			if (this.messengerElement) {
				//this.messengerContainer.appendChild(this.messengerElement);
				//this.player.setContainerA(this.messengerElement);
			}
			if (this.quizElement) {
				removeAllChildNodes(this.quizContainer);

				this.player.setQuizActive(true);
			}
			if (this.messengerContainer) {
				this.messengerContainer.classList.remove("h-100");
			}

			this.loadingVisible(false);
			this.unavailableVisible(false);
			this.playerVisible(true);
		}
		else {
			if (this.messengerElement) {
				//this.messengerContainer.appendChild(this.messengerElement);
			}
			if (this.quizElement) {
				this.quizContainer.appendChild(this.quizElement);
			}
			if (this.messengerContainer) {
				this.messengerContainer.classList.add("h-100");
			}

			this.playerVisible(false);
			this.unavailableVisible(true);
		}

		window.dispatchEvent(new Event('resize'));
	}

	stompMessageToMessageObject(stompMessage) {
		const messageBody = stompMessage.body;
		return JSON.parse(messageBody);
	}

	async onChatMessageReceive(message) {
		var url = new URL("https://" + window.location.host + "/course/messenger/messageReceived");
		var params = {timestamp: message.time, content: message.text, from: message.username};

		Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));

		const response = await fetch(url, {
			method: "GET",
		})
		.then((response) => {
			return response.text();
		})
		.then(html => {
			if (html) {
				const doc = new DOMParser().parseFromString(html, "text/html");

				const chatHistory = this.messengerElement.querySelector("#chat-history-list");

				if (chatHistory) {
					for (const child of doc.body.children) {
						chatHistory.appendChild(child);
					}
				}

				chatHistory.scrollTop = chatHistory.scrollHeight;
			}
		})
		.catch(error => console.error(error));


		return response;
	}

	onPlayerSettings() {
		window.enumerateDevices(true)
			.then(result => {
				this.showDeviceChooserModal(result.devices, result.stream, result.constraints, false);
			})
			.catch(error => {
				console.error(error);

				if (error.name == "NotReadableError") {
					window.enumerateDevices(false)
						.then(result => {
							this.showDeviceChooserModal(result.devices, result.stream, result.constraints, true);
						})
						.catch(error => {
							console.error(error);
						});
				}
				else if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
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
		if (localStorage.getItem("audioinput") === null) {
			this.showDeviceInitModal();
		}
		else {
			this.sendSpeechRequest();
		}
	}

	cancelSpeech() {
		if (!this.speechRequestId) {
			this.player.setRaiseHand(false);
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

	cancelSpeechRequest() {
		
	}

	createMessengerButtonForPlayer(callback){
		const messengerButton = document.createElement("button");
		messengerButton.setAttribute('title', 'Messenger öffnen/schließen');
		messengerButton.innerHTML = '<i class="bi bi-messenger"></i>';
		messengerButton.addEventListener("click", callback);
		return messengerButton;
	}

	displayIsWideEnoughForSidePanel () {
		const width = Math.max(
			document.documentElement.clientWidth,
			window.innerWidth || 0
		)
		return width >= 992
	}
	  
	async loadMessenger() {
		await fetch("/course/messenger/" + this.courseId, {
			method: "GET",
		})
		.then((response) => {
			return response.text();
		})
		.then(html => {
			if (html) {
				const doc = new DOMParser().parseFromString(html, "text/html");

				this.messengerElement = document.createElement("div");
				this.messengerElement.classList.add("h-100");

				for (const child of doc.body.children) {
					this.messengerElement.appendChild(child);
				}

				if (this.player) {
					const messengerButton = this.createMessengerButtonForPlayer(() => {
						this.togglMessenger();
					});
					this.player.addToolbarElement(messengerButton);
				}
				this.messengerContainer.appendChild(this.messengerElement);
				if(this.player){
					this.hideMessenger();
				}				

				this.initMessenger();
			}
		})
		.catch(error => console.error(error));
		this.establishMessengerConnection();
	}

	async reloadMessengerHistory() {
		const submitButton = this.messengerElement.querySelector("#messageSubmit");
		const chatHistory = this.messengerElement.querySelector("#chat-history");
		const chatHistoryList = this.messengerElement.querySelector("#chat-history-list");

		this.setMessengerForm(true);
		chatHistory.classList.add("hidden");

		await fetch("/course/messenger/history/" + this.courseId, {
			method: "GET",
		})
		.then((response) => {
			return response.text();
		})
		.then( async json => {
			const messengerHistoryDto = JSON.parse(json);
			const messengerHistory = messengerHistoryDto.messengerHistory;

			for (let mm of messengerHistory) {
				await this.onChatMessageReceive(mm);
			}
		})
		chatHistory.classList.remove("hidden");
		chatHistoryList.scrollTop = chatHistoryList.scrollHeight;
		this.setMessengerForm(false);
	}

	attachMessengerToModal(){
		this.messengerModal = mountInModal(this.messengerContainer, this.messengerElement, 'Nachricht an Dozent/in versenden', () => {
			this.hideMessenger();
		});
	}

	attachMessengerToSidePanel(){
		this.player.setContainerA(this.messengerElement);
	}

	messengerResize(){
		if(! this.messengerIsVisible){
			return;
		}
		if(! this.player){
			return;
		}
		if(this.messengerModal != null){
			return;
		}
		if(! this.displayIsWideEnoughForSidePanel()){
			this.hideMessenger();
		}
	}

	showMessenger(){
		if(this.player){
			if(this.displayIsWideEnoughForSidePanel()){
				this.attachMessengerToSidePanel();
			}else{
				this.attachMessengerToModal();
				this.messengerModal.show();
			}
		}
		
		this.elementVisible(this.messengerElement, true);
		this.messengerIsVisible = true;
	}

	messengerIsMountedInMessengerContainer(){
		return this.messengerContainer.children.length > 0;
	}

	hideMessenger(){
		if(this.messengerModal != null){
			this.messengerModal.hide();
		}
		this.elementVisible(this.messengerElement, false);
		if(! this.messengerIsMountedInMessengerContainer()){
			this.messengerContainer.appendChild(this.messengerElement);
		}
		this.messengerModal = null;
		if(this.player != null){
			this.player.setContainerA(null);
		}
		this.messengerIsVisible = false;
	}

	togglMessenger(){
		if(this.messengerIsVisible){
			this.hideMessenger();
		}else{
			this.showMessenger();
		}
	}

	messageWasSent(){
		if(this.messengerModal != null){
			this.hideMessenger();
		}
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
			this.setMessengerForm(true);

			const data = new FormData(event.target);
			const value = Object.fromEntries(data.entries());
			if (this.stompClient.connected) {
				this.sendOverSTOMP(value);
				this.showToast("toast-success", "course.feature.message.sent");
			}
			else {
				this.showToast("toast-warn", "course.feature.message.send.error");
				this.loadMessenger();
			}

			messageForm.reset();
			const messageTextArea = messageForm.querySelector("#messageTextarea");
			messageTextArea.setAttribute("style", "resize: none; overflow:hidden");
			this.setMessengerForm(false);
			submitButton.disabled = false;
			this.messageWasSent();
		}).catch(error => console.error(error));
	}

	sendOverSTOMP(value) {
		this.stompClient.send("/app/message/" + this.courseId, {}, JSON.stringify(value));
	}

	setMessengerForm(disabled) {
		if (this.messengerElement) {
			const messageSubmitButton = this.messengerElement.querySelector("#messageSubmit");
			messageSubmitButton.disabled = disabled;
		}
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
			this.player.setQuizActive(true);
		}
		else {
			this.quizContainer.appendChild(this.quizElement);
		}
	}

	initQuizModal() {
		const quizModalElement = document.getElementById("quizModal");
		const quizModalContent = document.getElementById("quizModalContent");

		const quizModal = bootstrap.Modal.getOrCreateInstance(quizModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const hiddenHandler = () => {
			quizModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);

			this.player.setShowQuiz(false);

			removeAllChildNodes(quizModalContent);

			quizModal.dispose();
		};

		quizModalElement.addEventListener("hidden.bs.modal", hiddenHandler);
		quizModalContent.appendChild(this.quizElement);

		quizModal.show();
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

		const speechModal = bootstrap.Modal.getOrCreateInstance(speechModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const cancelHandler = () => {
			this.cancelSpeech();
		};
		const startHandler = () => {
			this.player.startSpeech(speechConstraints);

			speechModal.hide();
		};
		const hiddenHandler = () => {
			speechModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			cancelButton.removeEventListener("click", cancelHandler);
			startButton.removeEventListener("click", startHandler);
			speechModal.dispose();

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

		speechModal.show();
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

	showDeviceChooserModal(devices, stream, constraints, camBlocked) {
		const audioInputs = devices.filter(device => device.kind === "audioinput");
		const videoInputs = devices.filter(device => device.kind === "videoinput");

		const cameraBlockedAlert = document.getElementById("cameraBlockedModalAlert");
		const cameraSelect = document.getElementById("cameraSelect");
		const microphoneSelect = document.getElementById("microphoneSelect");
		const saveButton = document.getElementById("saveDeviceSelection");
		const cancelButton = document.getElementById("deviceSaveCancel");
		const meterCanvas = document.getElementById("meter");
		
		const video = document.getElementById("cameraPreview");
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
		cameraSelect.options.length = 1;

		microphoneSelect.onchange = onAudioDeviceChange;
		microphoneSelect.options.length = 0;

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

		const deviceModalElement = document.getElementById("deviceModal");
		const deviceModal = bootstrap.Modal.getOrCreateInstance(deviceModalElement, {
			backdrop: "static",
			keyboard: false
		});

		const cancelHandler = () => {
			
		};
		const saveHandler = () => {
			const form = document.getElementById("deviceSelectForm");
			const data = new FormData(form);
			const devices = Object.fromEntries(data.entries());

			window.saveDeviceChoice(devices);

			deviceModal.hide();
		};
		const hiddenHandler = () => {
			deviceModalElement.removeEventListener("hidden.bs.modal", hiddenHandler);
			cancelButton.removeEventListener("click", cancelHandler);
			saveButton.removeEventListener("click", saveHandler);
			deviceModal.dispose();

			window.stopMediaTracks(video.srcObject);
		};

		cancelButton.addEventListener("click", cancelHandler);
		saveButton.addEventListener("click", saveHandler);
		deviceModalElement.addEventListener("hidden.bs.modal", hiddenHandler);
		deviceModalElement.addEventListener("shown.bs.modal", () => {
			const audioTrack = stream.getAudioTracks()[0];

			window.getAudioLevel(audioTrack, meterCanvas);
		});

		if (camBlocked) {
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

	getUserDevices(options) {
		navigator.mediaDevices.getUserMedia({ audio: true, video: (options.video ? true : false) })
			.then(stream => {
				const settings = {
					audioInput: null,
					videoInput: null
				};

				const audioTrack = stream.getAudioTracks()[0];
				const videoTrack = stream.getVideoTracks()[0];

				if (audioTrack) {
					settings.audioInput = audioTrack.getSettings().deviceId;
				}
				if (videoTrack) {
					settings.videoInput = videoTrack.getSettings().deviceId;
				}

				window.stopMediaTracks(stream);
				window.saveDeviceChoice(settings);

				this.sendSpeechRequest();
			})
			.catch(error => {
				console.error(error.name);

				if (error.name == "NotReadableError") {
					this.showDevicePermissionDeniedModal();
				}
				else if (error.name == "NotAllowedError" || error.name == "PermissionDeniedError") {
					this.showDevicePermissionDeniedModal();
				}
			});
	}

	loadingVisible(visible) {
		this.elementVisible(this.loadingContainer, visible);
	}

	unavailableVisible(visible) {
		this.elementVisible(this.unavailableContainer, visible & !(this.player || this.messengerElement || this.quizElement));
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

	isPlayerUsed(){
		if(this.isPlayerUsedContainer != null){
			return this.isPlayerUsedContainer.checked;
		}
		return true;
	}
}

window.course = new Course();

var modalId = 0;
function mountInModal(root, element, title = null, hiddenCallback = null){
	let html = `<div class="modal" tabindex="-1">
	<div class="modal-dialog">
		<div class="modal-content">`;
	if(title != null){
		html += `<div class="modal-header">
		<h5 class="modal-title">${title}</h5>
		<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
		</div>`
	}
	const id = modalId++;
	
	html += `<div class="modal-body" id="modal-content-${id}"></div>
		</div>
	</div>
	</div>`;
	root.innerHTML = html;
	const modalContent = document.getElementById('modal-content-' + id);
	modalContent.appendChild(element);
	console.log(modalContent);
	console.log(element);
	const modal =  new bootstrap.Modal(root.firstChild);

	if(hiddenCallback != null){
		root.firstChild.addEventListener('hidden.bs.modal', hiddenCallback);
	}

	return modal;
}