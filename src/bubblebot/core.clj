(ns bubbel.core
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))

(def server {:host "irc.freenode.net"
             :port 6667
             :channels ["###bubbletest"]})
(def user {:name "Too Much Bubble" :nick "bubbel-test"})

(declare conn-handler)

(defn connect [server]
  (let [socket (Socket. (:host server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out})]
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn write [conn msg]
  "Write a raw message to IRC server"
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (println msg)
      (cond 
       (re-find #"^ERROR :Closing Link:" msg)
        (dosync (alter conn merge {:exit true}))
       (re-find #"^PING" msg)
        (write conn (str "PONG "  (re-find #":.*" msg)))))))

(defn login [conn user]
  "Login and perform initial commands"
  (write conn (str "NICK " (:nick user)))
  (write conn (str "USER " (:nick user) " 0 * :" (:name user)))
  (doseq [chan (:channels server)] (write conn (str "JOIN " chan))))

(def irc (connect server))
(login irc user)
