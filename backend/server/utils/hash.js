let bcrypt = require("bcryptjs");

let hashNewPassword = password => {
	return new Promise((resolve, reject) => {
		bcrypt.genSalt(10, (error, salt) => {
			bcrypt.hash(password, salt, (error, hash) => {
				password = hash;
				resolve(password);
			});
		});
	});
};

module.exports = { hashNewPassword };
