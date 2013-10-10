(ns bubblebot.core
  (:import (java.net Socket))
  (:require [clojure.string :refer [split trim]]
            [clojure.java.io :as io]
            [bubblebot.irc-cmd :as cmd]
            [bubblebot.urler :as urler]))

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
  "Return a function to write a raw message to given connection `conn`.
  The returned function takes either a message or a collection of messages."
  [conn]
  (fn write [msg]
    (if (coll? msg)
      (doseq [m msg] (write m))
      (do (println (str "-> " msg))
          (binding [*out* (:out @conn)] (println msg))))))

(defn create-connection
  "Create an IRC socket and return a map with reader and writer."
  [server]
  (let [socket (Socket. (:host server) (:port server))]
    (ref {:in (io/reader socket) :out (io/writer socket)})))

(defn apply-cbs
  [lines cbs]
  (doseq [line (map parse-line lines)]
    (doseq [cb cbs] (cb line))))

(defn cb-ping-pong
  [line]
  (when (= (:cmd line) "PING") (cmd/pong (:raw line))))

(defn cb-println
  [line]
  (println (str "<- " (:raw line))))

(defn make-cb-wrapper
  "Return a function to wrap callbacks. The callback will be run in a
  new thread, and the return value will be passed to `write`."
  [write]
  (fn [cb] (fn [line] (if-let [l (cb line)] (write l)))))

(defn connect
  [server user cbs]
  (let [conn (create-connection server)
        write (writer conn)
        cbs (map (make-cb-wrapper write) cbs)]
    (.start (Thread. #(apply-cbs (line-seq (:in @conn)) cbs)))
    (write (cmd/register-user user (:channels server)))))

(defn -main [& args]
  ; TODO: Read from config/args
  (let [server {:host "irc.freenode.net" :port 6667
                :channels ["###555" "###bubbletest"]}
        user {:name "Too Much Bubble" :nick "bubbel"}]
    (connect server user [ cb-ping-pong cb-println urler/listen ])))
