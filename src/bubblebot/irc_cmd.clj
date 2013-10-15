(ns bubblebot.irc-cmd
  "Builds IRC message strings according to the
  IRC Client Protocol <http://tools.ietf.org/html/rfc2812>")

(defn nick [n] (str "NICK " n))

(defn user
  "Used at the beginning of a connection to specify the username,
  mode and realname of a new user."
  [u rn] (str "USER " u " 0 * :" rn))

(defn join
  "Join a given channel, with an optional key."
  ([c] (str "JOIN " c))
  ([c k] (str "JOIN " c " " k)))

(defn msg
  "Send a message to a user or a channel."
  [t m] (str "PRIVMSG " t " :" m))

(defn part
  "Part a given channel."
  [c] (str "PART " c))

(defn pong
  "Takes a PONG line and returns the PING reply."
  [p] (.replace p "PING" "PONG"))

(defn quit
  "Quit the IRC server, with an optional quit message."
  ([] "QUIT")
  ([m] (str "QUIT :" m)))

(defn register-user
  "Commands sent to IRC server on first connection."
  ([u]
   [(nick (:nick u))
    (user (:nick u) (:name u))])
  ([u chans]
   [(nick (:nick u))
    (user (:nick u) (:name u))
    (map join chans)]))
