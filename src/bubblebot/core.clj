(ns bubblebot.core
  (:import (java.net Socket))
  (:require [clojure.string :refer [split trim]]
            [clojure.java.io :as io]
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

(defn writer
  "Return a function to write a raw message to given connection."
  [conn]
    (fn [msg] (binding [*out* (:out @conn)] (println msg))))

(defn create-connection
  "Create an IRC socket and return a map with reader and writer."
  [server]
  (let [socket (Socket. (:host server) (:port server))]
    (ref {:in (io/reader socket) :out (io/writer socket)})))

(defn register-user
  "Login and perform initial commands."
  [conn user server]
  (let [write (writer conn)]
    (write (cmd/nick (:nick user)))
    (write (cmd/user (:nick user) (:name user)))
    (doseq [chan (:channels server)] (write (cmd/join chan)))))

(defn conn-handler
  [conn listeners]
  "Listen to .readLine from `conn`"
  (let [write (writer conn)]
    ; XXX: Basically while (true) and a break statement. Looks ugly?
    (while (nil? (:exit @conn))
      (let [line (parse-line (.readLine (:in @conn)))]
        (println (:cmd line) (:msg line))
        (listeners line conn)
        (cond
         (= (:cmd line) "ERROR")
          (dosync (alter conn merge {:exit true}))
         (= (:cmd line) "PING")
          (write (cmd/pong (:raw line))))))))

(defn -main [& args]
  ; TODO: Read from config/args
  (let [server {:host "irc.freenode.net" :port 6667
                :channels ["###bubbletest"]}
        user   {:name "Too Much Bubble" :nick "bubbel-test"}
        conn   (create-connection server)
        handler #(conn-handler conn bubblebot.urler/listen)]
    ; Start parsing IRC connection outstream in new thread
    (.start (Thread. handler))
    (register-user conn user server)))
