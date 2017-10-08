require("./config/config");

const express = require("express");
const bodyParser = require("body-parser");
const http = require("http");
const socketIO = require("socket.io");
let app = express();
let server = http.createServer(app);
let io = socketIO(server);

const { mongoose } = require("./db/mongoose");

const userRouter = require("./routes/userRoutes");
const venueRouter = require("./routes/venueRoutes");

// middleware
app.use(bodyParser.json());
app.use("/static", express.static(__dirname + "/uploads"));

// define routes
app.use("/api/user", userRouter);
app.use("/api/venues", venueRouter);

// Chatroom
let numUsers = 0;

io.on("connection", socket => {
	let addedUser = false;

	// listener when client emits 'new message'
	socket.on("new message", data => {
		// tell the client to execute 'new message'
		socket.broadcast.emit("new message", {
			username: socket.username,
			message: data
		});
	});

	// listener when client emits 'add user'
	socket.on("add user", username => {
		if (addedUser) return;

		// store the username in the socket session for this client
		socket.username = username;
		++numUsers;
		addedUser = true;
		socket.emit("login", {
			numUsers: numUsers
		});
		// echo globally (all clients) that a person has connected
		socket.broadcast.emit("user joined", {
			username: socket.username,
			numUsers: numUsers
		});
	});

	// listener when client emits 'typing', broadcast it to others
	socket.on("typing", () => {
		socket.broadcast.emit("typing", {
			username: socket.username
		});
	});

	// listener when client emits 'stop typing', broadcast it to others
	socket.on("stop typing", () => {
		socket.broadcast.emit("stop typing", {
			username: socket.username
		});
	});

	// listener when the user disconnects
	socket.on("disconnect", () => {
		if (addedUser) {
			--numUsers;

			// echo globally that this client has left
			socket.broadcast.emit("user left", {
				username: socket.username,
				numUsers: numUsers
			});
		}
	});
});

// set port and listen to it
const port = process.env.PORT;
server.listen(port, () => {
	console.log(`[INFO] Listening on port ${port}`);
});

module.exports = { app };
