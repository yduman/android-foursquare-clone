const express = require("express");
const request = require("request");

let makeApiRequest = url => {
	return new Promise((resolve, reject) => {
		request(url, (error, response, body) => {
			if (error || response.statusCode === 400 || response.statusCode === 404) {
				reject(response.statusCode);
			} else {
				resolve(JSON.parse(body));
			}
		});
	});
}

module.exports = { makeApiRequest };
