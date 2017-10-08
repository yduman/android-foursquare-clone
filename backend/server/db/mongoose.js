let mongoose = require("mongoose");

// mongoose config
mongoose.Promise = global.Promise;
mongoose.connect(process.env.MONGODB_URI, {useMongoClient: true});

module.exports = { mongoose };
