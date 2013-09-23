'use strict';

var path = require('path');
var config = require('./config2');
var _ = require('underscore');

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
 * Quote plugin
 */
var QuoteDB = require('./quotes');

var quoter = new QuoteDB({
  db_url: config.quotes.db.url,
  db_name: config.quotes.db.name,
  db_port: config.quotes.db.port,
  db_auth: config.quotes.db.auth
});

var commands = {
  INVALID: -1,
  GETQUOTEBYNUM: 1,
  GETRANDQUOTE: 2,
  SEARCHQUOTES: 3,
  GETLASTQUOTE: 4,
  ADDQUOTE: 5
};

var parseQuoteCommand = function (text) {
  var parsed = /^!(\w+)(.*)?/.exec(text);
  if (parsed == null) return [ commands.INVALID ];
  var cmd = parsed[1];
  var arg = parsed[2];
  if (cmd === 'q') {
    if (arg == null || !arg.trim()) return [ commands.GETRANDQUOTE ];
    arg = arg.trim();
    if (arg == parseInt(arg, 10)) {
      return [ commands.GETQUOTEBYNUM, parseInt(arg, 10) ];
    }
    return [ commands.SEARCHQUOTES, arg ];
  } else if (cmd === 'qlast') {
    return [ commands.GETLASTQUOTE ];
  } else if (cmd === 'addquote') {
    return [ commands.ADDQUOTE, arg ];
  }
};

bot.on('message#', function (nick, channel, text) {
  if (text[0] !== '!') return;
  var p = parseQuoteCommand(text);
  if (p == null) return;
  var cmd = p[0];
  var arg = p[1];
  if (cmd >= commands.GETQUOTEBYNUM && cmd <= commands.GETLASTQUOTE) {
    var cb = function (err, quotes) {
      if (err != null) return;
      if (quotes.length === 0) {
        return bot.say(channel, 'No match for "'+arg+'"');
      }
      var quote = quotes[0];
      if (quotes.length > 1) {
        var nums = _.pluck(quotes, 'num');
        bot.say(channel, quotes.length + ' matches: '+nums.join(', '));
        // Random
        quote = quotes[Math.floor(Math.random()*quotes.length)];
      }
      bot.say(channel, '[\u0002'+quote.num+'\u0002] '+quote.text);
    };
    switch (cmd) {
      case commands.GETQUOTEBYNUM: quoter.getByNum(arg, cb); break;
      case commands.GETRANDQUOTE: quoter.getRand(cb); break;
      case commands.SEARCHQUOTES: quoter.search(arg, cb); break;
      case commands.GETLASTQUOTE: quoter.getLast(cb); break;
      default: break;
    }
  } else if (cmd === commands.ADDQUOTE) {
    quoter.add(arg, nick, function (err, num) {
      if (err != null) return bot.say(channel, 'Failed to add quote: '+err.message);
      bot.say(channel, '\u0002'+num+'\u0002 added');
    });
  }
});

bot.connect();
