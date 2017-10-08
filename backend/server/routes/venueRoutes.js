const express = require("express");
const request = require("request");
const multer = require("multer");
const path = require("path");
const _ = require("lodash");
const router = express.Router();

const { authenticate } = require("./../middleware/authenticate");
const { makeApiRequest } = require("./../utils/request");
const { Venue } = require("./../models/venue");

// Foursquare API set up (provide your own secrets)
const clientId = "someClientId";
const secretId = "someSecretId";
const version = "someVersion";
const apiBaseUrl = "https://api.foursquare.com/v2/venues/";
const urlSuffix = `&venuePhotos=1&client_id=${clientId}&client_secret=${secretId}&v=${version}`;

// multer configuration
const storage = multer.diskStorage({
	destination: function(req, file, cb) {
		cb(null, "server/uploads");
	},
	// filename = venueId-date.jpg
	filename: function(req, file, cb) {
		cb(null, req.params.id + "-" + Date.now() + ".jpg");
	}
});

const upload = multer({ storage: storage }).single("venue");

// for populating database with venues
async function updateDatabase(groups) {
	for (let i = 0; i < groups.length; i++)
		await Venue.checkForMissingVenuesOrUpdate(groups[i].items);
}

/**
 * get venues nearby from the Foursquare API
 */
router.get("/explore/near/:near", async (req, res) => {
	try {
		const near = req.params.near;
		const url = `${apiBaseUrl}explore?near=${near}${urlSuffix}`;
		const json = await makeApiRequest(url);

		updateDatabase(json.response.groups);

		res.send(json);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get venues nearby with query from the Foursquare API
 */
router.get("/explore/near/:near/query/:query", async (req, res) => {
	try {
		const near = req.params.near;
		const query = req.params.query;
		const url = `${apiBaseUrl}explore?near=${near}&query=${query}${urlSuffix}`;
		const json = await makeApiRequest(url);

		updateDatabase(json.response.groups);

		res.send(json);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get venues nearby for specific category from the Foursquare API
 */
router.get("/explore/near/:near/category/:category", async (req, res) => {
	try {
		const near = req.params.near;
		const category = req.params.category;
		const url = `${apiBaseUrl}explore?near=${near}&section=${category}${urlSuffix}`;
		const json = await makeApiRequest(url);

		updateDatabase(json.response.groups);

		res.send(json);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * returns check-ins history for a user
 */
router.post("/checkins", authenticate, async (req, res) => {
	try {
		const body = _.pick(req.body, ["checkIns"]);
		const venues = await Venue.getCheckedInVenues(body);
		res.json({ venues });
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * get venue by id from database
 */
router.get("/:id", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		res.send(venue);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * post comment for venue
 */
router.post("/:id/comment", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		const body = _.pick(req.body, ["text", "image"]);
		body["username"] = req.user.username;

		const updatedVenue = venue.createComment(body);
		res.send(updatedVenue);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * like / dislike comments for venue
 */
router.patch("/:id/comment", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		const body = _.pick(req.body, ["likes", "pos", "isLike"]);
		body["username"] = req.user.username;
		venue.updateComment(body);

		res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * check if user already liked/disliked comment
 */
router.get("/:id/comment/:pos", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const pos = req.params.pos;
		const username = req.user.username;

		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		const hasRated = venue.checkIfUserRatedComment(pos, username);
		if (hasRated) res.status(204).send();
		else res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * upload image for venue
 */
router.post("/:id/comment/upload", authenticate, (req, res) => {
	try {
		upload(req, res, async function(error) {
			const id = req.params.id;
			const venue = await Venue.findById(id);

			if (!venue) res.status(404).send();
			if (error) res.status(400).send();

			venue.addPhoto(req.file.filename);

			res.send(req.file.filename);
		});
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * rate venue
 */
router.post("/:id/rating", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const username = req.user.username;

		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		const body = _.pick(req.body, ["rating"]);
		const updatedVenue = venue.updateRating(body.rating, username);

		res.send(updatedVenue);
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * check if user already rated venue
 */
router.get("/:id/rating", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const username = req.user.username;

		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		const hasRated = venue.checkIfUserRated(username);
		if (hasRated) res.status(204).send();
		else res.status(200).send();
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * upload image for venue
 */
router.post("/:id/upload", authenticate, (req, res) => {
	try {
		upload(req, res, async function(error) {
			const id = req.params.id;
			const venue = await Venue.findById(id);

			if (!venue) res.status(404).send();
			if (error) res.status(400).send();

			venue.addPhoto(req.file.filename);

			res.status(200).send();
		});
	} catch (error) {
		res.status(400).send();
	}
});

/**
 * update checkins for venue
 */
router.patch("/:id/checkins", authenticate, async (req, res) => {
	try {
		const id = req.params.id;
		const username = req.user.username;

		const venue = await Venue.findById(id);
		if (!venue) res.status(404).send();

		const body = _.pick(req.body, ["checkIns"]);
		const updatedVenue = venue.updateCheckIns(body.checkIns, username);

		res.send(updatedVenue);
	} catch (error) {
		res.status(400).send();
	}
});

module.exports = router;
