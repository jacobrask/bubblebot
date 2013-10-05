(ns bubblebot.core
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  (:require [bubblebot.irc-cmd :as irc]))

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

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (println (str "> " msg))
      (let [[_ from priv chan cmd] (re-find #":(.*)!~.* (PRIVMSG) (.*) :(.*)" msg)]
        (if-not nil? cmd) (println (str "> " cmd)))
      (cond 
       (re-find #"^ERROR :Closing Link:" msg)
        (dosync (alter conn merge {:exit true}))
       (re-find #"^PING" msg)
        (writer conn (irc/pong (re-find #":.*" msg)))))))

(defn login
  "Login and perform initial commands."
  [conn user]
  (writer conn (irc/nick (:nick user)))
  (writer conn (irc/user (:nick user) (:name user)))
  (doseq [chan (:channels server)] (writer conn (irc/join chan))))

(defn -main
  "Connect and login"
  [& args]
  (def irc (connect server))
  (login irc user))
