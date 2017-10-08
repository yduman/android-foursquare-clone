let { expect } = require("chai");
let request = require("supertest");

let { app } = require("../server");
let { Venue } = require("../models/venue");
let { users, venues, populateVenues, populateUsers } = require("./seed/seed");

beforeEach(populateVenues);
beforeEach(populateUsers);

describe("==> Venue Tests\n", () => {
	describe("GET /api/venues/explore/near/:near", () => {
		it("should return venues nearby for given location as string", done => {
			request(app)
				.get("/api/venues/explore/near/Frankfurt Hauptbahnhof")
				.expect(200)
				.expect(res => {
					expect(res.body.response.geocode.where).to.equal(
						"frankfurt hauptbahnhof"
					);
				})
				.end(done);
		});

		it("should return venues nearby for given location as lat and long", done => {
			request(app)
				.get("/api/venues/explore/near/50.103943, 8.665820")
				.expect(200)
				.expect(res => {
					expect(res.body.response.geocode.where).to.equal(
						"50.103943, 8.665820"
					);
				})
				.end(done);
		});
	});

	describe("GET /api/venues/explore/near/:near/query/:query", () => {
		it("should return venues for given search term", done => {
			request(app)
				.get("/api/venues/explore/near/:Frankfurt am Main/query/Burger King")
				.expect(200)
				.expect(res => {
					expect(res.body.response.query).to.equal("burger king");
					expect(res.body.response.geocode.where).to.equal("frankfurt am main");
				})
				.end(done);
		});
	});

	describe("GET /api/venues/explore/near/:near/category/:category", () => {
		it("should return venues for given category", done => {
			request(app)
				.get("/api/venues/explore/near/:Frankfurt am Main/category/coffee")
				.expect(200)
				.expect(res => {
					expect(res.body.response.query).to.equal("coffee");
					expect(res.body.response.geocode.where).to.equal("frankfurt am main");
				})
				.end(done);
		});
	});

	describe("GET /api/venues/:id", () => {
		it("should return venue for given id", done => {
			let id = venues[0]._id;
			request(app)
				.get(`/api/venues/${id}`)
				.set("x-auth", users[0].tokens[0].token)
				.expect(200)
				.expect(res => {
					expect(res.body._id).to.equal(venues[0]._id);
				})
				.end(error => {
					if (error) return done(error);

					Venue.findById(id)
						.then(venue => {
							expect(venue).to.exist;
							expect(venue._id).to.be.id;
							done();
						})
						.catch(error => done(error));
				});
        });
            
        it("should return 404 if venue not found", done => {
            let id = "foobar";
            request(app)
				.get(`/api/venues/${id}`)
				.set("x-auth", users[0].tokens[0].token)
                .expect(404)
                .end(done);
        });
	});
});
