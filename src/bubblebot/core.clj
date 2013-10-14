(ns bubblebot.core
  (:import (java.net Socket))
  (:require [clojure.string :refer [split trim]]
            [clojure.java.io :as io]
            [bubblebot.irc-cmd :as cmd]
            [bubblebot.line-parser :as parser]
            [bubblebot.urler :as urler]))

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
  (doseq [line (map parser/parse lines)]
    (doseq [cb cbs] (cb line))))

(defn cb-ping-pong
  [line]
  (when (= (:command line) "PING") (cmd/pong (:raw line))))

(defn cb-println
  [line]
  (println (str "<- " (:raw line))))

(defn make-cb-wrapper
  "Return a function to wrap callbacks. The callback will be run in a
  new thread, and the return value will be passed to `write`."
  [write]
  (fn [cb] (fn [line] (if-let [l (cb line)] (write l)))))

(defn connect
  [server user channels cbs]
  (let [conn (create-connection server)
        write (writer conn)
        cbs (map (make-cb-wrapper write) cbs)]
    (.start (Thread. #(apply-cbs (line-seq (:in @conn)) cbs)))
    (write (cmd/register-user user channels))
    conn))

(defn disconnect
  [conn]
  ((writer conn) (cmd/quit)))

(defn -main
  ([] (-main "config.clj"))
  ([conf-file]
   (let [cfg (read-string (slurp conf-file))]
     (connect (:server cfg) (:user cfg) (:channels cfg)
              [ cb-ping-pong cb-println urler/listen ]))))
