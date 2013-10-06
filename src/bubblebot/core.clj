(ns bubblebot.core
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  (:require [clojure.string :refer [split trim]]
            [bubblebot.irc-cmd :as cmd]))

; TODO: Read from config
(def server {:host "irc.freenode.net"
             :port 6667
             :channels ["###bubbletest"]})
(def user {:name "Too Much Bubble" :nick "bubbel-test"})

(declare conn-handler)

(defn connect
  "Connect to IRC server."
  [server]
  (let [socket (Socket. (:host server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out})]
    (.start (Thread. #(conn-handler conn)))
    conn))

(defn writer
  "Return a function which writes a raw message to server connection `conn`."
  [conn]
    (fn [msg]
      (doto (:out @conn)
        (.println (str msg "\r"))
        (.flush))))

; TODO: Split out parser to a separate namespace, and make it more comprehensible.
(def RE-LINE #"^(\:\S+.*?|)([^\: ]\S+.*?|)([^\: ]\S+.*?|)(\:\S+.*?|)$")
(defn parse-line
  "Parses raw IRC input to a map."
  [line]
  (let [drop-semicolon (fn [s] (if (.startsWith (str s) ":") (drop 1 s) s))
        trimmed-line (for [g (drop 1 (re-find RE-LINE line))] (trim g))
        cleaned-line (map #(apply str (drop-semicolon %)) trimmed-line)]
    (assoc (zipmap [:prefix :cmd :params :msg] cleaned-line) :raw line)))

; Should this function take a list of callbacks?
(defn conn-handler [conn]
  (let [write (writer conn)]
    ; XXX: Basically while (true) and a break statement. Looks ugly.
    (while (nil? (:exit @conn))
      (let [line (parse-line (.readLine (:in @conn)))]
        (println (:raw line))
        (cond
         (re-find #"^ERROR :Closing Link:" (:raw line))
          (dosync (alter conn merge {:exit true}))
         (= (:cmd line) "PING")
          (write (cmd/pong (:raw line))))))))

(defn login
  "Login and perform initial commands."
  [conn user]
  (let [write (writer conn)]
    (write (cmd/nick (:nick user)))
    (write (cmd/user (:nick user) (:name user)))
    (doseq [chan (:channels server)] (write (cmd/join chan)))))

(defn -main [& args]
  ; connect could take an additional list of callbacks.
  ; The callbacks comes from loading a number of "plugins"/features.
  ; That way a single instance can run multiple bots, using different settings
  ; and plugins.
  (let [conn (connect server)]
    (login conn user)))
