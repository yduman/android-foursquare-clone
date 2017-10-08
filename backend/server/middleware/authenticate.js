let { User } = require("../models/user");

// middleware for authentication
let authenticate = (request, response, next) => {
	let token = request.header("x-auth");

	User.findByToken(token)
		.then(user => {
			if (!user) return Promise.reject();

			request.user = user;
			request.token = token;
			next();
		})
		.catch(() => {
			response.status(401).send(); // send 401 for unauthorized
		});
};

module.exports = { authenticate };
