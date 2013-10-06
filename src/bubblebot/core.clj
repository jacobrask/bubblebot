(ns bubblebot.core
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  (:require [clojure.string :refer [split trim]]
            [bubblebot.irc-cmd :as cmd]))

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
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn writer
  "Write a raw message to IRC server"
  [conn msg]
  (println (str "< " msg))
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(def RE-LINE #"^(\:\S+.*?|)([^\: ]\S+.*?|)([^\: ]\S+.*?|)(\:\S+.*?|)$")

(defn parse-line
  "Parses raw IRC input to a map."
  [line]
  (let [drop-semicolon (fn [s] (if (.startsWith (str s) ":") (drop 1 s) s))
        trimmed-line (for [g (drop 1 (re-find RE-LINE line))] (trim g))
        cleaned-line (map #(apply str (drop-semicolon %)) trimmed-line)]
    (assoc (zipmap [:prefix :cmd :params :msg] cleaned-line) :raw line)))

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (let [line (parse-line (.readLine (:in @conn)))]
      (println (:raw line))
      (cond 
       (re-find #"^ERROR :Closing Link:" (:raw line))
        (dosync (alter conn merge {:exit true}))
       (= (:cmd line) "PING")
        (writer conn (cmd/pong (:raw line)))))))

(defn login
  "Login and perform initial commands."
  [conn user]
  (writer conn (cmd/nick (:nick user)))
  (writer conn (cmd/user (:nick user) (:name user)))
  (doseq [chan (:channels server)] (writer conn (cmd/join chan))))

(defn -main [& args]
  (login (connect server) user))
