let { ObjectID } = require("mongodb");
let jwt = require("jsonwebtoken");

let { User } = require("../../models/user");
let { Venue } = require("../../models/venue");

let userId1 = new ObjectID();
let userId2 = new ObjectID();
let userId3 = new ObjectID();

let users = [
	{
		_id: userId1,
		email: "foo1@foo1.com",
		password: "user1passwd",
		username: "foobar",
		firstname: "foo",
		lastname: "bar",
		age: 21,
		domicile: "Somewhere over the rainbow",
		secQuestion1: "Cleveland Cavaliers",
		secQuestion2: "Maiden Name of Mother",
		friends: ["foobar2"],
		tokens: [
			{
				access: "auth",
				token: jwt.sign({ _id: userId1, access: "auth" }, "SomeSalt").toString()
			}
		]
	},
	{
		_id: userId2,
		email: "foo2@foo2.com",
		password: "user2passwd",
		username: "foobar2",
		firstname: "foo2",
		lastname: "bar2",
		age: 21,
		domicile: "Somewhere over the rainbow",
		secQuestion1: "Golden State Warriors",
		secQuestion2: "Another Maiden Name of Mother",
		friends: ["foobar"],
		friendRequests: ["foobar3"],
		tokens: [
			{
				access: "auth",
				token: jwt.sign({ _id: userId2, access: "auth" }, "SomeSalt").toString()
			}
		]
	},
	{
		_id: userId3,
		email: "foo3@foo3.com",
		password: "user3passwd",
		username: "foobar3",
		firstname: "foo3",
		lastname: "bar3",
		age: 21,
		domicile: "Somewhere over the rainbow",
		secQuestion1: "asd",
		secQuestion2: "asd",
		tokens: [
			{
				access: "auth",
				token: jwt.sign({ _id: userId3, access: "auth" }, "SomeSalt").toString()
			}
		]
	}
];

let venues = [
	{
		_id: "4b0fc082f964a520986423e3",
		name: "MoschMosch",
		photos: [{}],
		ratings: [],
		comments: [],
		categories: [
			{
				shortName: "Noodles",
				name: "Noodle House",
				id: "4bf58dd8d48988d1d1941735",
				_id: new ObjectID()
			}
		],
		location: {
			formattedAddress: [
				"Wilhelm-Leuschner-Str. 78",
				"60329 Frankfurt am Main",
				"Deutschland"
			]
		},
		contact: {
			phone: "+496924003737"
		},
		hours: {
			isOpen: true
		},
		price: {
			message: "Moderate"
		}
	},
	{
		_id: "4c0gd082g964b520986423f3",
		name: "Fake MoschMosch",
		photos: [{}],
		ratings: [],
		comments: [],
		categories: [
			{
				shortName: "Noodles",
				name: "Noodle House",
				id: "4af58dd8d48988d1d1941735",
				_id: new ObjectID()
			}
		],
		location: {
			formattedAddress: [
				"Wilhelm-Leuschner-Str. 78",
				"60329 Frankfurt am Main",
				"Deutschland"
			]
		},
		contact: {
			phone: "+496924003737"
		},
		hours: {
			isOpen: true
		},
		price: {
			message: "Moderate"
		}
	}
];

const populateUsers = done => {
	User.remove({})
		.then(() => {
			let user1 = new User(users[0]).save();
			let user2 = new User(users[1]).save();
			let user3 = new User(users[2]).save();

			return Promise.all([user1, user2, user3]);
		})
		.then(() => done());
};

const populateVenues = done => {
	Venue.remove({})
		.then(() => {
			let venue1 = new Venue(venues[0]).save();
			let venue2 = new Venue(venues[1]).save();

			return Promise.all([venue1, venue2]);
		})
		.then(() => done());
};

module.exports = { users, populateUsers, venues, populateVenues };
