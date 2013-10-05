; Builds IRC message strings according to the
; IRC Client Protocol <http://tools.ietf.org/html/rfc2812>

(ns bubblebot.irc-cmd)

(defn nick [n] (str "NICK " n))

(defn user
  "The USER command is used at the beginning of connection to specify
   the username, hostname and realname of a new user."
  [user realname]
  (str "USER " user " 0 * " realname))

(defn join
  ([chan] (str "JOIN " chan))
  ([chan key] (str "JOIN " chan " " key)))

(defn part [chan] (str "PART " chan))

(defn pong [server] (str "PONG " chan))

(defn quit
  ([] "QUIT")
  ([msg] (str "QUIT :" msg)))
