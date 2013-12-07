# Bubblebot

An IRC bot with a simple plugin system.

To get started, copy `config.sample.clj` to `config.clj`, make your changes and run `./run`. Leiningen is currently required.

# Plugins

A plugin is a function that takes an IRC message and (optionally) returns one or more messages.

The plugin needs to expose a `message-handler` function, which will be called for each message the server sends to the client. Any string or collection of strings returned will be sent to the IRC server.

See the default plugins in src/bubblebot/plugins.

To enable a plugin, add its namespace to the `plugins` map in `config.clj`.

The `bubblebot.msg-builder` namespace contains a number of functions that are useful to build IRC messages, such as `bubblebot.msg-builder/msg` which returns a PRIVMSG string.

# License

Copyright © 2013 Jacob Rask

Distributed under the Eclipse Public License version 1.0
