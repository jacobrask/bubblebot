; Builds IRC message strings according to the
; IRC Client Protocol <http://tools.ietf.org/html/rfc2812>

(ns bubblebot.irc-cmd)

(defn nick [n] (str "NICK " n))

(defn user
  "Used at the beginning of a connection to specify the username,
  hostname and realname of a new user."
  [user realname]
  (str "USER " user " 0 * :" realname))

(defn join
  "Join a given channel, with an optional key."
  ([c] (str "JOIN " c))
  ([c k] (str "JOIN " c " " k)))

(defn msg
  "Send a message to a user or a channel."
  [target msg]
  (str "PRIVMSG " target " :" msg))

(defn part
  "Part a given channel."
  [chan]
  (str "PART " chan))

(defn pong
  "Takes a PONG line and returns the PING reply."
  [ping]
  (.replace ping "PING" "PONG"))

(defn quit
  "Quit the IRC server, with an optional quit message."
  ([] "QUIT")
  ([msg] (str "QUIT :" msg)))
