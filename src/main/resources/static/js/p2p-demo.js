class P2PDemo {

	nodes = null;
	edges = null;


	constructor() {
		this.initialize();
	}

	initialize() {
		this.nodes = new vis.DataSet([{
			id: "19ba501f-cd70-42ad-855b-8423d0b5c4a2",
			label: "S",
			color: "#F472B6",
			level: 0
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
			layout: {
				randomSeed: undefined,
				improvedLayout: true,
				clusterThreshold: 150,
				hierarchical: {
					enabled: false,
					levelSeparation: 150,
					nodeSpacing: 100,
					treeSpacing: 200,
					blockShifting: true,
					edgeMinimization: true,
					parentCentralization: true,
					sortMethod: 'hubsize',  // hubsize, directed
					shakeTowards: 'leaves'  // roots, leaves
				}
			}
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
			client.subscribe("/topic/p2p/document/done", (message) => {
				const client = JSON.parse(message.body);

				this.onDocumentLoaded(client);
			});
		};
		client.activate();

		window.addEventListener("beforeunload", () => {
			client.deactivate();
		});
	}

	onJoined(client) {
		console.log("joined", client);

		this.nodes.add({
			id: client.uid,
			label: client.type === "SUPER_PEER" ? "SP" : "P",
			level: client.type === "SUPER_PEER" ? 1 : 2,
			color: {
				border: client.type === "SUPER_PEER" ? "#A78BFA" : "#60A5FA",
				background: client.type === "SUPER_PEER" ? "#A78BFA" : "#60A5FA",
			}
		});

		for (const server of client.servers) {
			const bandwidth = Math.min(client.bandwidth, server.bandwidth);

			this.edges.add({
				from: client.uid,
				to: server.uid,
				value: bandwidth,
				title: "Bandwidth: " + bandwidth
			});
		}
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

	onDocumentLoaded(client) {
		console.log("loaded", this.nodes.get(client.uid));

		const node = this.nodes.get(client.uid);
		node.color = {
			border: "#059669",
		};

		this.nodes.update(node);
	}

	logClient(client) {
		const log = document.querySelector("#demo-log");
		const template = document.querySelector("#client-entry");
		const logElement = template.content.cloneNode(true);

		logElement.querySelector("#event-id").textContent = "Joined";
		logElement.querySelector("#client-id").textContent = client.uid;

		log.appendChild(logElement);
	}
}

window.p2pDemo = new P2PDemo();