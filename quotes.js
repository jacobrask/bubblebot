var cradle = require('cradle');

var QuoteDB = function (opts) {
  var conn = new cradle.Connection(opts.db_url, opts.db_port, { auth: opts.db_auth });
  this.db = conn.database(opts.db_name);
};

QuoteDB.prototype.getByNum = function (num, cb) {
  this.db.view('quote/text_from_num', { key: num }, function (err, doc) {
    if (err != null) return cb(err);
    if (doc.rows.length === 0) return cb(null, []);
    var quote = { num: num, text: doc.rows[0].value };
    cb(null, [ quote ]);
  });
};
QuoteDB.prototype.getRand = function (cb) {
  this.db.view('quote/text_from_num', { limit: 0 }, function (err, doc) {
    if (err != null) return cb(err);
    var total = doc.total_rows;
    var num = Math.floor(Math.random() * total) + 1;
    this.db.view('quote/text_from_num', { key: num }, function (err, doc) {
      if (err != null) return cb(err);
      if (doc.rows.length === 0) return cb(null, []);
      var quote = { num: num, text: doc.rows[0].value };
      cb(null, [ quote ]);
    });
  }.bind(this));
};
QuoteDB.prototype.getLast = function (cb) {
  this.db.view('quote/text_from_num', { limit: 1, descending: true }, function (err, doc) {
    if (err != null) return cb(err);
    if (doc.rows.length === 0) return cb(null, []);
    var quote = { num: doc.rows[0].key, text: doc.rows[0].value };
    cb(null, [ quote ]);
  });
};

QuoteDB.prototype.add = function (text, adder, cb) {
  this.db.view('quote/text_from_num', { limit: 1, descending: true }, function (err, doc) {
    if (err != null) return cb(err);
    var last = doc.rows[0].key;
    var quote = {
      text: text,
      adder: adder,
      num: last + 1,
      date: Date.now()
    };
    this.db.save(quote, function (err) {
      if (err != null) cb(err);
      cb(null, last + 1);
    });
  }.bind(this));
};

exports = module.exports = QuoteDB;
