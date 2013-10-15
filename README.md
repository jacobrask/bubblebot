# Bubblebot

An IRC bot/client with a simple plugin system.

To get started, copy `config.sample.clj` to `config.clj`, make your changes and run `./run`. Leiningen is currently required.

# Plugins

A plugin only needs a public function called `message-handler`. This function will be called for each message the server sends to the client. If the function returns a string or a collection of strings, they are sent to the IRC server.

The `message-handler` function gets one argument, a map with the fields `raw`, `cmd`, `params` and, if available, `nick`, `host` and `user`. Examples:

    {:cmd "366", :params ["bubblebot" "###bubbletest" "End of /NAMES list."], :raw ":calvino.freenode.net 366 bubblebot ###bubbletest :End of /NAMES list.", :nick "calvino.freenode.net"}
    {:cmd "PRIVMSG", :params ["###bubbletest" "hello bubblebot"], :raw ":me!~me@example.com PRIVMSG ###bubbletest :hello bubblebot", :host "example.com", :user "~me", :nick "me"}

To enable a plugin, add its namespace to the `plugins` map in `config.clj`.

# License

Copyright © 2013 Jacob Rask

Distributed under the Eclipse Public License version 1.0
