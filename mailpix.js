// Web Hook for inbound emails
var connect = require('connect');
var connectRoute = require('connect-route');
var events = require("events");
var http = require('http');
var _ = require('underscore');
var util = require('util');

var MailPix = function (opts) {
  events.EventEmitter.call(this);
  opts || (opts = {});
  this.port = opts.port || 80;
  this.listen();
};
util.inherits(MailPix, events.EventEmitter);

MailPix.prototype.listen = function () {
  var self = this;
  var app = connect();
  app.use(connect.json({ strict: true }));
  app.use(connectRoute(function (router) {
    router.get('/', function (req, res) { res.end(); });
    router.post('/mailpix', function (req, res, next) {
      var from = req.body.From;
      var subject = req.body.Subject;
      if (!from || !subject) return next(new Error('Not an email'));
      var to = subject.trim().toLowerCase();
      if (to[0] !== '#') to = '#'+to;
      var file = _.find(req.body.Attachments, function (file) {
        return ~file.ContentType.indexOf('image');
      });
      if (file == null) return next(new Error('No image attachment'));
      var image = {
        content: new Buffer(file.Content, 'base64'),
        name: file.Name,
        type: file.ContentType
      };
      self.emit('img', image, to, from);
      res.end();
    });
  }));
  app.use(function (err, req, res, next) {
    var ua = req.headers['user-agent'];
    var ip = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
    self.emit('error', err, ip, ua);
    res.writeHead(500);
    res.end();
  });
  http.createServer(app).listen(this.port);
};

module.exports = MailPix;
