let mongoose = require("mongoose");
let moment = require("moment");
require("mongoose-double")(mongoose);
let SchemaTypes = mongoose.Schema.Types;

let validator = require("validator");
let jwt = require("jsonwebtoken");
let _ = require("lodash");
let bcrypt = require("bcryptjs");
let fs = require("fs");

/**
 * the user schema
 */
let UserSchema = new mongoose.Schema({
	email: {
		type: String,
		required: true,
		trim: true,
		minlength: 1,
		unique: true,
		validate: {
			validator: value => validator.isEmail(value),
			message: "{VALUE} is not a valid email"
		}
	},
	password: {
		type: String,
		required: true,
		minlength: 6
	},
	username: {
		type: String,
		required: true,
		unique: true,
		trim: true,
		minlength: 1,
		maxlength: 11
	},
	firstname: {
		type: String,
		required: true,
		trim: true,
		minlength: 1
	},
	lastname: {
		type: String,
		required: true,
		trim: true,
		minlength: 1
	},
	age: {
		type: Number,
		required: true,
		min: 0,
		max: 150
	},
	domicile: {
		type: String,
		required: true,
		trim: true,
		minlength: 1
	},
	secQuestion1: {
		type: String,
		required: true,
		trim: true,
		minlength: 1
	},
	secQuestion2: {
		type: String,
		required: true,
		trim: true,
		minlength: 1
	},
	imagePath: {
		type: String,
		default: "no image"
	},
	lat: SchemaTypes.Double,
	lng: SchemaTypes.Double,
	friends: [String],
	friendRequests: [String],
	checkIns: [
		{
			venueId: String,
			checkInCount: Number,
			timestamp: String
		}
	],
	tokens: [
		{
			access: {
				type: String,
				required: true
			},
			token: {
				type: String,
				required: true
			}
		}
	]
});

let hasValue = (obj, key, val) => {
	return obj[key] === val;
};

/**
 * overriding toJSON to hide some data
 */
UserSchema.methods.toJSON = function() {
	let user = this;
	let userObject = _.pick(user.toObject(), [
		"_id",
		"email",
		"username",
		"firstname",
		"lastname",
		"age",
		"domicile",
		"friends",
		"friendRequests",
		"checkIns",
		"imagePath"
	]);

	let name = _.pick(userObject, ["firstname", "lastname"]);
	userObject["fullname"] = String(name["firstname"]) + " " + String(name["lastname"]);

	return userObject;
};

/**
 * creates an auth token for an user on login
 */
UserSchema.methods.createAuthToken = function() {
	let user = this;
	let access = "auth";
	let token = jwt.sign({ _id: user._id.toHexString(), access }, "SomeSalt").toString();

	user.tokens.push({ access, token });

	return user.save().then(() => {
		return token;
	});
};

/**
 * removes the auth token on logout
 */
UserSchema.methods.removeAuthToken = function(token) {
	let user = this;

	return user.update({
		$pull: {
			tokens: { token }
		}
	});
};

/**
 * saves profile image path on user object
 */
UserSchema.methods.setImage = function(path) {
	let user = this;

	user.imagePath = path;
	user.save();
};

/**
 * populates friend requests array, which contains usernames of the users sending the friendship requests
 */
UserSchema.methods.setFriendRequest = function(requesterUsername) {
	let requestedUser = this;

	if (!_.includes(requestedUser.friendRequests, requesterUsername))
		requestedUser.friendRequests.push(requesterUsername);

	requestedUser.save();
};

/**
 * returns all friends of the user
 */
UserSchema.methods.getAllFriends = function() {
	let user = this;
	return user.friends;
};

/**
 * returns all friend requests of the user
 */
UserSchema.methods.getAllFriendRequests = function() {
	let user = this;
	return user.friendRequests;
};

/**
 * removes an existing friend
 */
UserSchema.methods.removeFriend = function(usernameToRemove, userToRemove) {
	let user = this;

	let currUsername = user.username;

	// Remove username from both users friends array
	let indexFriends1 = user.friends.indexOf(usernameToRemove);
	let indexFriends2 = userToRemove.friends.indexOf(currUsername);
	if (indexFriends1 > -1) user.friends.splice(indexFriends1, 1);
	if (indexFriends2 > -1) userToRemove.friends.splice(indexFriends2, 1);

	// Remove username from both users friendRequests array
	let indexFriendRequests1 = user.friendRequests.indexOf(usernameToRemove);
	let indexFriendRequests2 = userToRemove.friendRequests.indexOf(currUsername);
	if (indexFriendRequests1 > -1) user.friendRequests.splice(indexFriendRequests1, 1);
	if (indexFriendRequests2 > -1)
		userToRemove.friendRequests.splice(indexFriendRequests2, 1);

	user.save();
	userToRemove.save();
};

/**
 * updates friendship based on accepting or rejecting friendship
 */
UserSchema.methods.updateFriends = function(requesterUsername, approval, requester) {
	let user = this;

	let currUserFriends = user.friends;
	let currUserFriendRequests = user.friendRequests;
	let currUsername = user.username;

	if (approval === "accept") {
		let index = currUserFriendRequests.indexOf(requesterUsername);
		if (index > -1) currUserFriendRequests.splice(index, 1);
		if (
			!_.includes(currUserFriends, requesterUsername) &&
			!_.includes(requester.friends, currUsername)
		) {
			currUserFriends.push(requesterUsername);
			requester.friends.push(currUsername);
			user.save();
			requester.save();
		}
	} else if (approval === "reject") {
		let index = currUserFriendRequests.indexOf(requesterUsername);
		if (index > -1) currUserFriendRequests.splice(index, 1);
		user.save();
	}
};

/**
 * updates check-ins of user
 */
UserSchema.methods.updateCheckIns = function(venueId) {
	let user = this;
	let hasVenue = user.checkIns.some(checkIn => hasValue(checkIn, "venueId", venueId));

	if (hasVenue) {
		let checkInObj = user.checkIns.find(checkIn => checkIn.venueId === venueId);
		checkInObj.checkInCount += 1;
		checkInObj.timestamp = moment().format("lll");
		user.save();
	} else {
		user.checkIns.push({
			venueId,
			checkInCount: 1,
			timestamp: moment().format("lll")
		});
		user.save();
	}
};

/**
 * saves the location of the user
 */
UserSchema.methods.saveLocation = function(lat, lng) {
	let user = this;

	user.lat = lat;
	user.lng = lng;

	user.save();
};

/**
 * gets the location of all friends
 */
UserSchema.methods.getFriendsLocation = async function() {
	let user = this;
	let locations = [];

	for (let username of user.friends) {
		const friend = await User.findByUsername(username);
		if (!friend) continue;
		let lat = friend.lat;
		let lng = friend.lng;
		locations.push({ lat, lng, username });
	}

	return locations;
};

/**
 * query to find a user by username
 */
UserSchema.statics.findByUsername = function(username) {
	let User = this;
	return User.findOne({ username }).then(user => {
		if (!user) return Promise.reject(new Error("Could not find user by username"));

		return new Promise((resolve, reject) => {
			resolve(user);
		});
	});
};

/**
 * query to find a user by token
 */
UserSchema.statics.findByToken = function(token) {
	let User = this;
	let decoded;

	try {
		decoded = jwt.verify(token, "SomeSalt");
	} catch (error) {
		return Promise.reject();
	}

	return User.findOne({
		_id: decoded._id,
		"tokens.token": token,
		"tokens.access": "auth"
	});
};

/**
 * query to find a user by email and password
 */
UserSchema.statics.findByCredentials = function(email, password) {
	let User = this;

	return User.findOne({ email }).then(user => {
		if (!user) return Promise.reject();

		return new Promise((resolve, reject) => {
			bcrypt.compare(password, user.password, (error, response) => {
				response ? resolve(user) : reject();
			});
		});
	});
};

/**
 * query to find a user by security questions
 */
UserSchema.statics.findBySecurityQuestions = function(email, secQuestion1, secQuestion2) {
	let User = this;

	return User.findOne({ email }).then(user => {
		if (!user) return Promise.reject();

		if (user.secQuestion1 !== secQuestion1) return Promise.reject();

		if (user.secQuestion2 !== secQuestion2) return Promise.reject();

		return new Promise((resolve, reject) => {
			resolve(user);
		});
	});
};

/**
 * query to find the venue king
 */
UserSchema.statics.findVenueKing = function(venueId) {
	let User = this;
	return User.aggregate([
		{ $unwind: "$checkIns" },
		{ $project: { "checkIns.venueId": 1, "checkIns.checkInCount": 1 } },
		{ $match: { "checkIns.venueId": venueId } },
		{ $sort: { "checkIns.checkInCount": -1 } },
		{ $limit: 1 }
	]).then(doc => {
		if (!doc) return Promise.reject();

		return new Promise((resolve, reject) => {
			resolve(doc[0]._id);
		});
	});
};

/**
 * returns all images of friends
 */
UserSchema.statics.getImagesOfFriends = async function(friends) {
	let User = this;

	let imagePaths = [];

	for (let username of friends) {
		const user = await User.findByUsername(username);
		if (!user) continue;
		if ("imagePath" in user) imagePaths.push({ username, imagePath: user.imagePath });
	}

	return imagePaths;
};

/**
 * a pre save hook to hash the password
 */
UserSchema.pre("save", function(next) {
	let user = this;

	if (user.isModified("password")) {
		bcrypt.genSalt(10, (error, salt) => {
			bcrypt.hash(user.password, salt, (error, hash) => {
				user.password = hash;
				next();
			});
		});
	} else {
		next();
	}
});

let User = mongoose.model("User", UserSchema);

module.exports = { User };
