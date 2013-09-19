var path = require('path');
var config = require('./config');

var irc = require('irc');
var bot = new irc.Client(config.server, config.nick, {
  autoConnect: false,
  channels: config.channels,
  userName: config.userName,
  realName: config.realName
});

var log = function (msg) {
  console.log(msg);
};

bot.on('registered', function (msg) {
  console.info('[IRC] Connected to ' + msg.server);
});
bot.on('join', function (channel, nick) {
  if (nick === config.nick) console.info('[IRC] Joined ' + channel);
});
bot.on('error', function (err) {
  log('[IRC]' + err);
});


/*
 * URLer plugin
 * Looks for valid URLs in messages and saves them to a database.
 */
var Urler = require('./urler');

var urler = new Urler({
  db_url: config.urler.db.url,
  db_name: config.urler.db.name,
  db_port: config.urler.db.port,
  db_auth: config.urler.db.auth
});

bot.on('message#', function (nick, channel, text) {
  urler.lookForUrl({ text: text, nick: nick, channel: channel });
});

urler.on('url', function (url, title, msg) {
  if (!title) return;
  urler.shorten(url, function (err, url) {
    bot.say(msg.channel, '\u0002'+title+'\u0002'+' ('+url+')');
  });
});

urler.on('urldupe', function (url, title, nicks, msg) {
  bot.say(msg.channel, url+' \u0002OLDNEWS\u0002 ('+nicks.join(', ')+')');
});

urler.on('error', function (err) { log('[URLER] '+err); });


/*
 * Mail Pix plugin
 * Recieves email via Postmark web hook, saves to S3 and prints URL.
 */
var MailPix = require('./mailpix');
var knox = require('knox');

var mailpic = new MailPix({
  port: process.env.PORT || 8080
});

var s3 = knox.createClient({
  key: config.mailpix.s3.key,
  secret: config.mailpix.s3.secret,
  bucket: config.mailpix.s3.bucket
});

mailpic.on('img', function (img, to, from) {
  if (bot.chans[to] == null) {
    return log('[MAILPIX] Error: Not in channel '+to+' (from '+from+')');
  }
  var headers = {
    'Content-Type': img.type,
    'Content-Length': img.content.byteLength
  };
  s3.putBuffer(img.content, path.join(config.mailpix.s3.path, img.name), headers, function (err) {
    if (err) return log('[MAILPIX] Upload failed: ' + err.message);
    var url = 'http://' + path.join(s3.bucket, s3.path, img.name);
    console.log('[MAILPIX] Uploaded ' + url + ' from ' + from);
    bot.say(to, '\u0002['+from+']\u0002 ' + url);
  });
});

mailpic.on('error', function (err, ip, ua) {
  log('[MAILPIX] '+err+', from '+ip+' ('+ua+')');
});

bot.connect();
