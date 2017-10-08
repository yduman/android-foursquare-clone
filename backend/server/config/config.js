let env = process.env.NODE_ENV || "development";

console.log("[INFO] Environment:", env);

// sets port to listen to and MongoDB URI based on environment the app is running on
if (env === "development") {
	process.env.PORT = 8080;
	process.env.MONGODB_URI = "mongodb://localhost:27017/Foursquare";
} else if (env === "test") {
	process.env.PORT = 3000;
	process.env.MONGODB_URI = "mongodb://localhost:27017/FoursquareTest";
} else if (env === "production") {
	process.env.PORT = 80;
	process.env.MONGODB_URI = "mongodb://localhost:27017/Foursquare";
}
