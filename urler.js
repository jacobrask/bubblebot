var cheerio = require('cheerio');
var cradle = require('cradle');
var events = require("events");
var request = require('request');
var tinyurl = require('nj-tinyurl');
var _ = require('underscore');
var util = require('util');
var urlsFrom = require('urlsfrom');


// Pretty lazy matching. If we match something that's not an URL, we notice
// when we try to GET it anyway.
var URL_RE = new RegExp('(https?:\\/\\/)?'+ // protocol
  '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // domain name
  '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
  '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
  '(\\?[;&a-z\\d%_.~+=-]*)?', 'gi'); // query string

var Urler = function (opts) {
  events.EventEmitter.call(this);
  var conn = new cradle.Connection(opts.db_url, opts.db_port, { auth: opts.db_auth });
  this.db = conn.database(opts.db_name);
  this.on('url', this.save);
  this.on('url', this.dupeCheck);
};
util.inherits(Urler, events.EventEmitter);

Urler.prototype.lookForUrl = function (msg) {
  var self = this;
  urlsFrom(msg.text, { title: true }, function (err, urls) {
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
      if (err) cb(err);
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
