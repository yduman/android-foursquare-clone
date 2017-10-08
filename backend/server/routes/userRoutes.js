const express = require("express");
const router = express.Router();

const _ = require("lodash");
const path = require("path");
const multer = require("multer");
const nodemailer = require("nodemailer");

const { ObjectID } = require("mongodb");
const { User } = require("./../models/user");
const { authenticate } = require("./../middleware/authenticate");
const { hashNewPassword } = require("./../utils/hash");

// multer configuration
const storage = multer.diskStorage({
	destination: function(req, file, cb) {
		cb(null, "server/uploads");
	},
	filename: function(req, file, cb) {
		cb(null, req.user.username + "-" + Date.now() + ".jpg");
	}
});
const upload = multer({ storage: storage }).single("avatar");

// nodemailer configuration (provide your own mail and pass)
let transporterMail = "someEmail";
let transporter = nodemailer.createTransport({
	service: "gmail",
	auth: {
		user: transporterMail,
		pass: "somePassword"
	}
});

let mailBodyRegistration = (userEmail, userName) => {
	let body = {
		from: transporterMail,
		to: `${userEmail}`,
		subject: "[Foursquare Clone] Welcome!",
		text: `
			Hi ${userName}!\n\n
			We just wanted to thank you for using the app! Have fun!\n\n
			Cheers,\n
			Foursquare Clone Team
		`
	};

	return body;
};

// It's bad practice, avoid this (lazy student here)
let mailBodyPasswordReset = (userEmail, userName, pass) => {
	let body = {
		from: transporterMail,
		to: `${userEmail}`,
		subject: "[Foursquare Clone] Account changes",
		text: `
			Greetings ${userName}!\n
			This message is a reminder for you.\n
			You should store this information somewhere secure and delete this message!\n\n
			Your credentials:\n
			e-mail: ${userEmail}
			password: ${pass}
			\n\n
			Sincerely,\n
			Foursquare Clone Team
		`
	};

	return body;
};

let sendMail = mail => {
	transporter.sendMail(mail, (error, info) => {
		if (error) console.log("[ERROR-MAIL] " + error);
		else console.log("[SUCCESS-MAIL] " + info.response);
	});
};

// **** START ROUTES **** //

/**
 * register new user
 */
router.post("/", async (req, res) => {
	try {
		const body = _.pick(req.body, [
			"email",
			"password",
			"username",
			"firstname",
			"lastname",
			"age",
			"domicile",
			"secQuestion1",
			"secQuestion2"
		]);
		const user = new User(body);
		await user.save();
		const token = await user.createAuthToken();
		res.header("x-auth", token).send(user);

		let mail = mailBodyRegistration(user.email, user.username);
		sendMail(mail);
	} catch (error) {
		res.status(400).send(error);
	}
});

/**
 * login user 
 */
router.post("/login", async (req, res) => {
	try {
		const body = _.pick(req.body, ["email", "password"]);
		const user = await User.findByCredentials(body.email, body.password);
		const token = await user.createAuthToken();
		res.header("x-auth", token).send(user);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get logged in user
 */
router.get("/me", authenticate, (req, res) => {
	res.send(req.user);
});

/**
 * forgot password
 */
router.patch("/me", async (req, res) => {
	try {
		let body = _.pick(req.body, [
			"email",
			"password",
			"secQuestion1",
			"secQuestion2"
		]);
		const email = body.email;
		const secQuestion1 = body.secQuestion1;
		const secQuestion2 = body.secQuestion2;
		const unhashedPassword = body.password;
		const hashedPassword = await hashNewPassword(unhashedPassword);
		body.password = hashedPassword;

		const user = await User.findOneAndUpdate(
			{ email, secQuestion1, secQuestion2 },
			{ $set: body },
			{ new: true }
		);
		if (!user) return res.status(404).send();

		res.send(user);

		let mail = mailBodyPasswordReset(user.email, user.username, unhashedPassword);
		sendMail(mail);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * logout and delete token
 */
router.delete("/me/token", authenticate, async (req, res) => {
	try {
		await req.user.removeAuthToken(req.token);
		res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * upload profile image
 */
router.post("/me/upload", authenticate, (req, res) => {
	upload(req, res, function(error) {
		if (error) res.status(400).send();

		const imagePath = req.file.filename;
		req.user.setImage(imagePath);
		res.send(imagePath);
	});
});

/**
 * send friend request
 */
router.post("/friends", authenticate, async (req, res) => {
	try {
		const body = _.pick(req.body, ["username"]);
		const requesterUsername = req.user.username;
		const requestedUsername = body.username;

		let requestedUser = await User.findByUsername(requestedUsername);
		if (!requestedUser) res.status(404).send();

		requestedUser.setFriendRequest(requesterUsername);

		res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get images of friends
 */
router.post("/friends/images", authenticate, async (req, res) => {
	try {
		const body = _.pick(req.body, ["friends"]);
		const friends = body.friends;

		const imagePaths = await User.getImagesOfFriends(friends);

		res.send({ imagePaths });
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * accept/reject friend request
 */
router.patch("/friends", authenticate, async (req, res) => {
	const body = _.pick(req.body, ["username", "approval"]);
	let requester = await User.findByUsername(body.username);
	if (!requester) res.status(404).send();

	req.user.updateFriends(body.username, body.approval, requester);

	res.status(200).send();
});

/**
 * save location of users
 */
router.post("/location", authenticate, async (req, res) => {
	try {
		const body = _.pick(req.body, ["lat", "lng"]);
		req.user.saveLocation(body.lat, body.lng);
		res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get friends location
 */
router.get("/friends/location", authenticate, async (req, res) => {
	try {
		const locations = await req.user.getFriendsLocation();
		res.json({ locations });
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get own profile picture
 */
router.get("/:username", authenticate, async (req, res) => {
	try {
		const username = req.params.username;
		const user = await User.findByUsername(username);

		if (!user) res.status(404).send();

		res.send(user);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * unfriend a friend
 */
router.delete("/friends/:username", authenticate, async (req, res) => {
	try {
		const username = req.params.username;

		const user = await User.findByUsername(username);
		if (!user) res.status(404).send();

		req.user.removeFriend(username, user);
		res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * edit user and reset password
 */
router.patch("/me/:id", authenticate, async (req, res) => {
	const id = req.params.id;
	let body = _.pick(req.body, [
		"email",
		"password",
		"username",
		"firstname",
		"lastname",
		"age",
		"domicile"
	]);

	if (!ObjectID.isValid(id)) return res.status(404).send();

	if (_.has(body, "password")) {
		try {
			const unhashedPassword = body.password;
			const hashedPassword = await hashNewPassword(unhashedPassword);
			body.password = hashedPassword;
			const user = await User.findOneAndUpdate(
				{ _id: id },
				{ $set: body },
				{ new: true }
			);
			if (!user) return res.status(404).send();
			res.send(user);

			let mail = mailBodyPasswordReset(user.email, user.username, unhashedPassword);
			sendMail(mail);
		} catch (error) {
			res.status(400).send();
		}
	} else {
		try {
			const user = await User.findOneAndUpdate(
				{ _id: id },
				{ $set: body },
				{ new: true }
			);
			if (!user) return res.status(404).send();
			res.send(user);
		} catch (error) {
			res.status(400).send();
		}
	}
});

/**
 * delete user
 */
router.delete("/me/:id", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		if (!ObjectID.isValid(id)) return res.status(404).send();

		const user = await User.findByIdAndRemove(id);
		if (!user) return res.status(404).send();

		res.send(user);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * update checkIns
 */
router.patch("/:id/checkIns", authenticate, async (req, res) => {
	try {
		const id = req.params.id; // venue id
		const user = req.user;

		user.updateCheckIns(id);

		res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get venue king
 */
router.get("/:id/checkIns/king", authenticate, async (req, res) => {
	try {
		const venueId = req.params.id;
		const userId = await User.findVenueKing(venueId);
		if (!ObjectID.isValid(userId)) return res.status(404).send();

		const user = await User.findById(userId);
		if (!user) return res.status(404).send();

		res.json({ username: user.username });
	} catch (error) {
		res.status(400).send(error);
	}
});

module.exports = router;
