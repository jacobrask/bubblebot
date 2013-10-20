# Bubblebot

An IRC bot/client with a simple plugin system.

To get started, copy `config.sample.clj` to `config.clj`, make your changes and run `./run`. Leiningen is currently required.

# Plugins

A plugin is a function that takes an IRC message and (optionally) returns one or more messages.

The plugin needs to expose a `message-handler` function, which will be called for each message the server sends to the client. Any string or collection of strings returned will be sent to the IRC server.

The plugin function gets one argument, a map with the fields `raw`, `cmd`, `params` and, if available, `nick`, `host` and `user`. Examples:

    {:cmd "366", :params ["bubblebot" "###bubbletest" "End of /NAMES list."], :raw ":calvino.freenode.net 366 bubblebot ###bubbletest :End of /NAMES list.", :nick "calvino.freenode.net"}
    {:cmd "PRIVMSG", :params ["###bubbletest" "hello bubblebot"], :raw ":me!~me@example.com PRIVMSG ###bubbletest :hello bubblebot", :host "example.com", :user "~me", :nick "me"}

To enable a plugin, add its namespace to the `plugins` map in `config.clj`.

The `bubblebot.msg-builder` namespace contains a number of functions that are useful to build IRC messages, such as `bubblebot.msg-builder/msg` which returns a PRIVMSG string.

# License

Copyright © 2013 Jacob Rask

Distributed under the Eclipse Public License version 1.0
