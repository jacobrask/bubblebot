(ns bubblebot.core
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  (:require [clojure.string :refer [split trim]]
            [bubblebot.irc-cmd :as cmd]
            [bubblebot.urler]))


; TODO: Split out parser to a separate namespace, and make it more comprehensible.
(def RE-LINE #"^(\:\S+.*?|)([^\: ]\S+.*?|)([^\: ]\S+.*?|)(\:\S+.*?|)$")
(defn parse-line
  "Given raw IRC input, returns a parsed map."
  [line]
  (let [drop-semicolon (fn [s] (if (.startsWith (str s) ":") (drop 1 s) s))
        trimmed-line   (for [g (drop 1 (re-find RE-LINE line))] (trim g))
        cleaned-line   (map #(apply str (drop-semicolon %)) trimmed-line)]
    (assoc (zipmap [:prefix :cmd :params :msg] cleaned-line) :raw line)))

(declare conn-handler)

(defn connect
  "Given a `host`/`port` map, create an IRC socket, returning a map with
  `in`/`out` streams."
  [server]
  (let [socket (Socket. (:host server) (:port server))
        in     (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out    (PrintWriter. (.getOutputStream socket))
        conn   (ref {:in in :out out})]
    conn))

(defn writer
  "Return a write function to write a raw message to given connection `conn`."
  [conn]
    (fn [msg]
      (doto (:out @conn)
        (.println (str msg "\r"))
        (.flush))))

(defn conn-handler
  [conn listeners]
  "Listen to .readLine from `conn`"
  (let [write (writer conn)]
    ; XXX: Basically while (true) and a break statement. Looks ugly?
    (while (nil? (:exit @conn))
      (let [line (parse-line (.readLine (:in @conn)))]
        (println (:raw line))
        (listeners line conn)
        (cond
         (re-find #"^ERROR :Closing Link:" (:raw line))
          (dosync (alter conn merge {:exit true}))
         (= (:cmd line) "PING")
          (write (cmd/pong (:raw line))))))))

(defn login
  "Login and perform initial commands."
  [conn user server]
  (let [write (writer conn)]
    (write (cmd/nick (:nick user)))
    (write (cmd/user (:nick user) (:name user)))
    (doseq [chan (:channels server)] (write (cmd/join chan)))))

(defn -main [& args]
  ; TODO: Read from config/args
  (let [server {:host "irc.freenode.net"
                :port 6667
                :channels ["###bubbletest"]}
        user   {:name "Too Much Bubble" :nick "bubbel-test"}
        conn   (connect server)
        handler #(conn-handler conn bubblebot.urler/listen)]
    ; Start parsing IRC connection outstream in new thread
    (.start (Thread. handler))
    (login conn user server)))
