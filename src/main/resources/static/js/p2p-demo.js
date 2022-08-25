class P2PDemo {

	nodes = null;
	edges = null;


	constructor() {
		this.initialize();
	}

	initialize() {
		this.nodes = new vis.DataSet([{
			id: 0,
			label: "S"
		}]);

		this.edges = new vis.DataSet([]);

		// Create a network
		const demoContainer = document.getElementById("demo-container");
		const container = document.getElementById("network");

		const data = {
			nodes: this.nodes,
			edges: this.edges,
		};
		const options = {
			height: "500px",
			width: "100%",
			nodes: {
				shape: "dot",
			},
		};
		const network = new vis.Network(container, data, options);

		const client = new StompJs.Client({
			brokerURL: "wss://" + window.location.host + "/p2p",
			reconnectDelay: 1000,
			heartbeatIncoming: 1000,
			heartbeatOutgoing: 1000,
		});
		client.onConnect = () => {
			client.subscribe("/topic/p2p/joined", (message) => {
				const client = JSON.parse(message.body);

				this.onJoined(client);
			});
			client.subscribe("/topic/p2p/left", (message) => {
				const client = JSON.parse(message.body);

				this.onLeft(client);
			});
		};
		client.activate();

		window.addEventListener("beforeunload", () => {
			client.deactivate();
		});
	}

	onJoined(client) {
		console.log("joined", client);

		const log = document.querySelector("#demo-log");
		const template = document.querySelector("#client-entry");
		const logElement = template.content.cloneNode(true);

		logElement.querySelector("#event-id").textContent = "Joined";
		logElement.querySelector("#client-id").textContent = client.uid;

		log.appendChild(logElement);

		this.nodes.add({
			id: this.nodes.length,
			label: "C"
		});
		this.edges.add({
			from: 0,
			to: this.nodes.length - 1,
			value: client.bandwidth,
			title: "Bandwidth: " + client.bandwidth
		});
	}

	onLeft(client) {
		console.log("left", client);

		const log = document.querySelector("#demo-log");
		const template = document.querySelector("#client-entry");
		const logElement = template.content.cloneNode(true);

		logElement.querySelector("#event-id").textContent = "Left";
		logElement.querySelector("#client-id").textContent = client.uid;

		log.appendChild(logElement);

		this.nodes.remove(this.nodes.length - 1);
	}
}

window.p2pDemo = new P2PDemo();