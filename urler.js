var cradle = require('cradle');
var events = require("events");
var tinyurl = require('nj-tinyurl');
var _ = require('underscore');
var urlsFrom = require('urlsfrom');
var util = require('util');

var Urler = function (opts) {
  events.EventEmitter.call(this);
  var conn = new cradle.Connection(opts.db_url, opts.db_port, { auth: opts.db_auth });
  this.db = conn.database(opts.db_name);
  this.on('url', this.save);
  this.on('url', this.dupeCheck);
};
util.inherits(Urler, events.EventEmitter);

Urler.prototype.lookForUrl = function (msg, cb) {
  var self = this;
  urlsFrom(msg.text, { title: true }, function (err, urls) {
    if (err != null) return cb(err);
    urls.forEach(function (url) {
      url.title = url.title.replace(/\s/g, ' ').replace(/(\s)\s+/g, ' ').trim();
      self.emit('url', url.href, url.title, msg);
    });
  });
};

Urler.prototype.shorten = function (url, cb) {
  if (url.length < 25) {
    cb(null, url);
  } else {
    tinyurl.shorten(url, function (err, shorturl) {
      if (err) cb(null, url);
      else cb(null, shorturl);
    });
  }
};

Urler.prototype.save = function (url, title, msg) {
  var urlData = msg;
  urlData.url = url;
  urlData.title = title;
  urlData.date = Date.now();
  this.db.save(urlData, function (err) {
    if (err) this.emit('error', err);
  }.bind(this));
};

Urler.prototype.dupeCheck = function (url, title, msg) {
  this.db.view('url/by_url', { key: url }, function (err, doc) {
    if (err) return this.emit('error', err);
    if (doc == null || doc.rows == null) return;
    var vals = _.pluck(doc.rows, 'value');
    vals = _.where(vals, { channel: msg.channel });
    var nicks = _.uniq(_.pluck(vals, 'nick'));
    // Ignore current user's nick.
    nicks = _.without(nicks, msg.from);
    if (nicks.length > 0) this.emit('urldupe', url, title, nicks, msg);
  }.bind(this));
};

exports = module.exports = Urler;
