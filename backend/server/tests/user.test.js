let { expect } = require("chai");
let request = require("supertest");
let { ObjectID } = require("mongodb");

let { app } = require("../server");
let { User } = require("../models/user");
let { users, populateUsers } = require("./seed/seed");

beforeEach(populateUsers);

// ------------------------------ GET CALLS ------------------------------ //

describe("==> User Tests\n", () => {
	describe("GET /api/user/me", () => {
		it("should return user if authenticated", done => {
			request(app)
				.get("/api/user/me")
				.set("x-auth", users[0].tokens[0].token)
				.expect(200)
				.expect(res => {
					expect(res.body._id).to.equal(users[0]._id.toHexString());
					expect(res.body.email).to.equal(users[0].email);
					expect(res.body.username).to.equal(users[0].username);
					expect(res.body.firstname).to.equal(users[0].firstname);
					expect(res.body.lastname).to.equal(users[0].lastname);
					expect(res.body.age).to.equal(users[0].age);
					expect(res.body.domicile).to.equal(users[0].domicile);
					expect(res.body.friends).to.be.not.empty;
				})
				.end(done);
		});

		it("should return 401 if not authenticated", done => {
			request(app)
				.get("/api/user/me")
				.expect(401)
				.expect(res => {
					expect(res.body).to.be.empty;
				})
				.end(done);
		});
	});

	describe("GET /api/user/:username", () => {
		it("should return user by given username", done => {
			request(app)
				.get(`/api/user/${users[0].username}`)
				.set("x-auth", users[2].tokens[0].token)
				.expect(200)
				.expect(res => {
					expect(res.body._id).to.equal(users[0]._id.toHexString());
					expect(res.body.email).to.equal(users[0].email);
					expect(res.body.username).to.equal(users[0].username);
					expect(res.body.firstname).to.equal(users[0].firstname);
					expect(res.body.lastname).to.equal(users[0].lastname);
					expect(res.body.age).to.equal(users[0].age);
					expect(res.body.domicile).to.equal(users[0].domicile);
					expect(res.body.friends).to.be.not.empty;
				})
				.end(done);
		});

		it("should return 400, if user not found", done => {
			request(app)
				.get("/api/user/someusername")
				.set("x-auth", users[2].tokens[0].token)
				.expect(400)
				.end(done);
		});
	});

	// ------------------------------ POST CALLS ------------------------------ //

	describe("POST /api/user", () => {
		it("should create a user", done => {
			let email = "example@example.com";
			let password = "foobar123";
			let username = "asd";
			let firstname = "asd";
			let lastname = "asd";
			let age = 21;
			let domicile = "asd";
			let secQuestion1 = "secQuestionAnswer1";
			let secQuestion2 = "secQuestionAnswer2";

			request(app)
				.post("/api/user")
				.send({
					email,
					password,
					username,
					firstname,
					lastname,
					age,
					domicile,
					secQuestion1,
					secQuestion2
				})
				.expect(200)
				.expect(res => {
					expect(res.headers["x-auth"]).to.exist;
					expect(res.body._id).to.exist;
					expect(res.body.email).to.be.email;
				})
				.end(error => {
					if (error) return done(error);

					User.findOne({ email })
						.then(user => {
							expect(user).to.exist;
							expect(user.password).to.not.be.password; // password should be hashed
							expect(user.username).to.be.username;
							expect(user.firstname).to.be.firstname;
							expect(user.lastname).to.be.lastname;
							expect(user.age).to.be.age;
							expect(user.domicile).to.be.domicile;
							expect(user.secQuestion1).to.be.secQuestion1;
							expect(user.secQuestion2).to.be.secQuestion2;
							done();
						})
						.catch(error => done(error));
				});
		});

		it("should throw validation errors if request is invalid", done => {
			request(app)
				.post("/api/user")
				.send({
					email: "someEmail",
					password: "somePassword",
					username: "someUserName",
					firstname: "someFirstName",
					lastname: "someLastName",
					age: 22,
					domicile: "someDomicile"
				})
				.expect(400)
				.end(done);
		});

		it("should not create user if email is in use", done => {
			request(app)
				.post("/api/user")
				.send({
					email: users[0].email,
					password: "somePassword",
					username: "someUserName",
					firstname: "someFirstName",
					lastname: "someLastName",
					age: 22,
					domicile: "someDomicile"
				})
				.expect(400)
				.end(done);
		});

		it("should not create user if username is in use", done => {
			request(app)
				.post("/api/user")
				.send({
					email: "foobar@foobar.org",
					password: "someSecretPassword",
					username: users[0].username,
					firstname: "someFirstName",
					lastname: "someLastName",
					age: 22,
					domicile: "someDomicile"
				})
				.expect(400)
				.end(done);
		});
	});

	describe("POST /api/user/login", () => {
		it("should login user and return auth tokens", done => {
			request(app)
				.post("/api/user/login")
				.send({
					email: users[1].email,
					password: users[1].password
				})
				.expect(200)
				.expect(res => {
					expect(res.headers["x-auth"]).to.exist;
				})
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[1]._id)
						.then(user => {
							expect(user.tokens[1]).to.include({
								access: "auth",
								token: res.headers["x-auth"]
							});
							done();
						})
						.catch(error => done(error));
				});
		});

		it("should reject invalid login", done => {
			request(app)
				.post("/api/user/login")
				.send({
					email: users[1].email,
					password: users[1].password + "abc"
				})
				.expect(400)
				.expect(res => {
					expect(res.headers["x-auth"]).to.not.exist;
				})
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[1]._id)
						.then(user => {
							expect(user.tokens.length).to.equal(1);
							done();
						})
						.catch(error => done(error));
				});
		});
	});

	describe("POST /api/user/me/upload", () => {
		it("should upload image", done => {
			request(app)
				.post("/api/user/me/upload")
				.set("x-auth", users[0].tokens[0].token)
				.attach("avatar", "server/tests/seed/seed_picture.png")
				.expect(200)
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[0]._id.toHexString())
						.then(user => {
							expect(user.imagePath).to.not.be.empty;
							done();
						})
						.catch(error => done(error));
				});
		});
	});

	describe("POST /api/user/friends", () => {
		it("should send a friend request", done => {
			request(app)
				.post("/api/user/friends")
				.set("x-auth", users[2].tokens[0].token)
				.send({
					username: users[0].username
				})
				.expect(200)
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[0]._id.toHexString())
						.then(user => {
							expect(user.friendRequests).to.include(users[2].username);
							done();
						})
						.catch(error => done(error));
				});
		});

		it("should return 400, if user not found", done => {
			request(app)
				.post("/api/user/friends")
				.set("x-auth", users[2].tokens[0].token)
				.send({
					username: "someusername"
				})
				.expect(400)
				.end(done);
		});
	});

	// ------------------------------ PATCH CALLS ------------------------------ //

	describe("PATCH /api/user/me", () => {
		it("should return user by given email", done => {
			let password = "someNewPassword";
			request(app)
				.patch("/api/user/me")
				.send({
					email: users[0].email,
					password: password,
					secQuestion1: users[0].secQuestion1,
					secQuestion2: users[0].secQuestion2
				})
				.expect(200)
				.expect(res => {
					expect(res.body._id).to.equal(users[0]._id.toHexString());
					expect(res.body.email).to.equal(users[0].email);
					expect(res.body.password).to.not.equal(password);
				})
				.end(done);
		});

		it("should return 404 if no email given", done => {
			request(app).patch("/api/user/me").send({}).expect(404).end(done);
		});

		it("should return 404 if not a valid email address", done => {
			request(app)
				.patch("/api/user/me")
				.send({
					email: "foobar",
					password: "foobar",
					secQuestion1: "foobar1",
					secQuestion2: "foobar2"
				})
				.expect(404)
				.end(done);
		});

		it("should return 404 if one security question is invalid", done => {
			request(app)
				.patch("/api/user/me")
				.send({
					email: users[0].email,
					password: "foobar",
					secQuestion1: "Cleveland Cavaliers",
					secQuestion2: "idk man"
				})
				.expect(404)
				.end(done);
		});

		it("should return 404 if all security questions are invalid", done => {
			request(app)
				.patch("/api/user/me")
				.send({
					email: users[0].email,
					password: "foobar",
					secQuestion1: "idk man",
					secQuestion2: "idk man either"
				})
				.expect(404)
				.end(done);
		});
	});

	describe("PATCH /api/user/me/:id", () => {
		it("should update an user", done => {
			let hexId = users[0]._id.toHexString();
			let email = "updated@mail.com";
			let username = "updatedUserName";
			let originalFirstname = users[0].firstname;
			let originalLastname = users[0].lastname;
			let age = 55;
			let domicile = "updatedDomicile";

			request(app)
				.patch(`/api/user/me/${hexId}`)
				.set("x-auth", users[0].tokens[0].token)
				.send({
					email: email,
					username: username,
					firstname: originalFirstname,
					lastname: originalLastname,
					age: age,
					domicile: domicile
				})
				.expect(200)
				.expect(res => {
					expect(res.body.email).to.be.email;
					expect(res.body.username).to.be.username;
					expect(res.body.firstname).to.be.originalFirstname;
					expect(res.body.lastname).to.be.originalLastname;
					expect(res.body.age).to.be.age;
					expect(res.body.age).to.be.an("number");
					expect(res.body.domicile).to.be.domicile;
				})
				.end(done);
		});

		it("should return 404 if user not found", done => {
			request(app)
				.patch(`/api/user/me/${new ObjectID().toHexString()}`)
				.set("x-auth", users[0].tokens[0].token)
				.expect(404)
				.end(done);
		});

		it("should return 404 if invalid id", done => {
			request(app)
				.patch("/api/user/me/123foobar")
				.set("x-auth", users[0].tokens[0].token)
				.expect(404)
				.end(done);
		});
	});

	describe("PATCH /api/user/friends", () => {
		it("should accept friend request", done => {
			request(app)
				.patch("/api/user/friends")
				.set("x-auth", users[1].tokens[0].token)
				.send({
					username: users[2].username,
					approval: "accept"
				})
				.expect(200)
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[1]._id.toHexString())
						.then(user => {
							expect(user.friends).to.include(users[2].username);
							expect(user.friendRequests).to.be.empty;
							done();
						})
						.catch(error => done(error));
				});
		});

		it("should reject friend request", done => {
			request(app)
				.patch("/api/user/friends")
				.set("x-auth", users[1].tokens[0].token)
				.send({
					username: users[2].username,
					approval: "reject"
				})
				.expect(200)
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[1]._id.toHexString())
						.then(user => {
							expect(user.friends).to.not.include(users[2].username);
							expect(user.friendRequests).to.be.empty;
							done();
						})
						.catch(error => done(error));
				});
		});
	});

	// ------------------------------ DELETE CALLS ------------------------------ //

	describe("DELETE /api/user/me/token", () => {
		it("should remove auth token on logout", done => {
			request(app)
				.delete("/api/user/me/token")
				.set("x-auth", users[0].tokens[0].token)
				.expect(200)
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[0]._id)
						.then(user => {
							expect(user.tokens.length).to.equal(0);
							done();
						})
						.catch(error => done(error));
				});
		});
	});

	describe("DELETE /api/user/me/:id", () => {
		it("should delete an user", done => {
			let hexId = users[0]._id.toHexString();

			request(app)
				.delete(`/api/user/me/${hexId}`)
				.set("x-auth", users[0].tokens[0].token)
				.expect(200)
				.expect(res => {
					expect(res.body._id).to.be.hexId;
				})
				.end((error, res) => {
					if (error) return done(error);

					User.findById(hexId)
						.then(user => {
							expect(user).to.not.exist;
							done();
						})
						.catch(error => done(error));
				});
		});

		it("should return 404 if user not found", done => {
			request(app)
				.patch(`/api/user/me/${new ObjectID().toHexString()}`)
				.set("x-auth", users[0].tokens[0].token)
				.expect(404)
				.end(done);
		});

		it("should return 404 if invalid id", done => {
			request(app)
				.patch("/api/user/me/123foobar")
				.set("x-auth", users[0].tokens[0].token)
				.expect(404)
				.end(done);
		});
	});

	describe("DELETE /api/user/friends/:username", () => {
		it("should unfriend a friend", done => {
			let username = users[0].username;

			request(app)
				.delete(`/api/user/friends/${username}`)
				.set("x-auth", users[1].tokens[0].token)
				.expect(200)
				.end((error, res) => {
					if (error) return done(error);

					User.findById(users[1]._id)
						.then(user => {
							expect(user.friends).to.not.include(username);
							done();
						})
						.catch(error => done(error));
				});
		});
	});
});
