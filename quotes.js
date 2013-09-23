var cradle = require('cradle');
var reds = require('reds');

var QuoteDB = function (opts) {
  var conn = new cradle.Connection(opts.db_url, opts.db_port, { auth: opts.db_auth });
  this.db = conn.database(opts.db_name);
  this.reds = reds.createSearch('quotes');
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
      text: text.trim(),
      adder: adder,
      num: last + 1,
      date: Date.now()
    };
    this.db.save(quote, function (err, res) {
      if (err != null) return cb(err);
      cb(null, last + 1);
      this.reds.index(text, res.id);
    }.bind(this));
  }.bind(this));
};

QuoteDB.prototype.search = function (term, cb) {
  this.reds.query(term).end(function (err, ids) {
    if (err != null) return cb(err);
    this.db.get(ids, function (err, docs) {
      if (err != null) return cb(err);
      var quotes = docs.map(function (doc) {
        console.log(doc);
        return { num: doc.num, text: doc.text };
      });
      cb(null, quotes);
    });
  }.bind(this));
};

exports = module.exports = QuoteDB;
