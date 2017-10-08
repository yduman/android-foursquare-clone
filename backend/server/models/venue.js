let mongoose = require("mongoose");
let moment = require("moment");
let _ = require("lodash");
require("mongoose-double")(mongoose);
let SchemaTypes = mongoose.Schema.Types;

/**
 * the venue schema
 */
let VenueSchema = new mongoose.Schema({
	_id: { type: String, required: true, unique: true },
	name: String,
	phone: String,
	formattedAddress: [String],
	categories: [
		{
			id: String,
			name: String,
			shortName: String
		}
	],
	priceMessage: String,
	isOpen: Boolean,
	checkIns: Number,
	lastCheckInUsername: String,
	lastCheckInTimestamp: String,
	comments: [
		{
			username: String,
			comment: String,
			image: String,
			likes: Number,
			likedBy: [String],
			dislikedBy: [String]
		}
	],
	likes: Number,
	dislikes: Number,
	ratings: [
		{
			rating: SchemaTypes.Double,
			ratedBy: String
		}
	],
	photos: [
		{
			prefix: String,
			width: Number,
			height: Number,
			suffix: String
		}
	]
});

/**
 * fetches only relevant information from the Foursquare API response
 * @param {JSON} venue 
 * @param {String} priceMessage 
 * @param {Boolean} isOpen 
 * @param {Array} photos 
 */
let getBody = (venue, priceMessage, isOpen, photos) => {
	return {
		_id: venue.id,
		name: venue.name,
		phone: venue.contact.phone,
		formattedAddress: venue.location.formattedAddress,
		categories: venue.categories,
		priceMessage,
		isOpen,
		checkIns: 0,
		photos
	};
};

/**
 * Checks if the price message field is existent in the API response.
 * If existent, set the value, otherwise set the value to undefined.
 * @param {JSON} venue 
 */
let getPriceMessageField = venue => {
	let message;

	if ("price" in venue) message = venue.price.message;
	else message = undefined;

	return message;
};

/**
 * Checks if the isOpen field is existent in the API response.
 * If existent, set the value, otherwiese set the value to undefined.
 * @param {JSON} venue 
 */
let getHoursIsOpenField = venue => {
	let isOpen;

	if ("hours" in venue) isOpen = venue.hours.isOpen;
	else isOpen = undefined;

	return isOpen;
};

/**
 * Checks if the photos field is existent in the API response.
 * If existent, set the values, otherwiese set the value to undefined.
 * @param {JSON} venue 
 */
let getPhotosField = venue => {
	if ("photos" in venue)
		return [
			{
				prefix: venue.photos.groups[0].items[0].prefix,
				width: venue.photos.groups[0].items[0].width,
				height: venue.photos.groups[0].items[0].height,
				suffix: venue.photos.groups[0].items[0].suffix
			}
		];
	else return undefined;
};

/**
 * updates existing venues in the database
 * @param {String} vId 
 * @param {JSON} vBody 
 */
let updateVenue = async (vId, vBody) => {
	await Venue.findOneAndUpdate({ _id: vId }, { $set: vBody }, { new: true });
};

/**
 * checks if the venue exists in the database.
 * if exists, then update the venue, otherwise create a new venue and save it.
 * @param {JSON} venue 
 */
let createOrUpdateVenue = async venue => {
	let message = getPriceMessageField(venue);
	let isOpen = getHoursIsOpenField(venue);
	let photos = getPhotosField(venue);

	let venueBody = getBody(venue, message, isOpen, photos);
	let venueId = venue.id;
	let doc = await Venue.findOne({ _id: venueId });

	if (!doc) {
		const v = new Venue(venueBody);
		v.save();
	} else {
		delete venueBody.checkIns;
		delete venueBody.photos;
		updateVenue(venueId, venueBody);
	}
};

let hasValue = (obj, key, val) => {
	return obj[key] === val;
};

/**
 * creates a comment
 */
VenueSchema.methods.createComment = function(body) {
	let venue = this;

	venue.comments.push({
		username: body.username,
		comment: body.text,
		image: body.image,
		likes: 0,
		dislikes: 0
	});
	venue.save();

	return venue;
};

/**
 * updates a comment
 */
VenueSchema.methods.updateComment = function(body) {
	let venue = this;
	let comment = venue.comments[body.pos];
	let username = body.username;

	if (!comment.likedBy.includes(username) && !comment.dislikedBy.includes(username)) {
		comment.likes = body.likes;
		if (body.isLike) {
			comment.likedBy.push(username);
		} else {
			comment.dislikedBy.push(username);
		}
	}

	venue.save();
};

/**
 * checks if the user already liked/disliked the comment
 */
VenueSchema.methods.checkIfUserRatedComment = function(pos, username) {
	let venue = this;
	let comment = venue.comments[pos];

	if (
		_.includes(comment.likedBy, username) ||
		_.includes(comment.dislikedBy, username)
	) {
		return true;
	} else {
		return false;
	}
};

/**
 * updates rating of venue
 */
VenueSchema.methods.updateRating = function(rating, ratedBy) {
	let venue = this;

	venue.ratings.push({ rating, ratedBy });
	venue.save();

	return venue;
};

/**
 * checks if the user already rated the venue
 */
VenueSchema.methods.checkIfUserRated = function(username) {
	let venue = this;
	return venue.ratings.some((rating) => hasValue(rating, "ratedBy", username));
};

/**
 * adds a photo for a venue
 */
VenueSchema.methods.addPhoto = function(suffix) {
	let venue = this;

	venue.photos.push({ suffix });
	venue.save();
};

/**
 * updates check-ins for a venue
 */
VenueSchema.methods.updateCheckIns = function(checkIns, username) {
	let venue = this;

	venue.checkIns = checkIns;
	venue.lastCheckInUsername = username;
	venue.lastCheckInTimestamp = moment().format('lll');

	venue.save();

	return venue;
};

/**
 * iterates over all venues and calls createOrUpdateVenue(venue: JSON)
 * this function is called for every Foursquare API request, to populate the database.
 */
VenueSchema.statics.checkForMissingVenuesOrUpdate = function(items) {
	let Venue = this;
	for (let i = 0; i < items.length; i++) createOrUpdateVenue(items[i].venue);
};

/**
 * returns all checked in venues
 */
VenueSchema.statics.getCheckedInVenues = async function(body) {
	let Venue = this;
	let result = [];
	let checkIns = body.checkIns;

	for (let i = 0; i < checkIns.length; i++) {
		const venue = await Venue.findById(checkIns[i].venueId);
		const timestamp = checkIns[i].timestamp;

		if (!venue) {
			continue;
		} else {
			result.push({ venue, timestamp });
		}
	}

	return result;
};

let Venue = mongoose.model("Venue", VenueSchema);
module.exports = { Venue };
