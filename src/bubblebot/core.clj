(ns bubblebot.core
  (:import (java.net Socket))
  (:require [clojure.java.io :as io]
            [bubblebot.irc-cmd :as cmd]
            [bubblebot.msg-parser :refer [parse]]))

(defn- writer
  "Return a function to write a raw message (or a collection of messages)
  to given connection `conn`"
  [conn]
  (fn write [msg]
    (when-not (empty? msg)
      (cond (coll? msg) (doseq [m msg] (write m))
            (string? msg) (do (println (str "-> " msg))
                              (binding [*out* (:out @conn)] (println msg)))))))

(defn- create-connection
  "Create an IRC socket and return a map with reader and writer"
  [{:keys [host port]}]
  (let [socket (Socket. host port)]
    (ref {:in (io/reader socket) :out (io/writer socket)})))

(defn- dodoseq
  "For each x, do all functions"
  [xs fs]
  (doseq [x xs] (doseq [f fs] (f x))))

(defn- cb-ping-pong [{:keys [cmd raw]}]
  (when (= cmd "PING") (cmd/pong raw)))

(defn connect
  "Connect to server and register plugin handlers"
  [server user channels handlers]
  (let [conn (create-connection server)
        write (writer conn)
        fns (map #(comp write %) handlers)
        lines (map parse (line-seq (:in @conn)))]
    (.start (Thread. #(dodoseq lines fns)))
    (write (cmd/register-user user channels))
    conn))

(defn disconnect [conn]
  ((writer conn) (cmd/quit)))

(defn require-plugins
  "Require plugins and return their message-handler functions"
  [plugins]
  (let [ps (map :ns (vals plugins))]
    (doseq [p ps] (require (symbol p)))
    (map (comp resolve symbol #(str % "/message-handler")) ps)))

(defn -main
  ([] (-main "config.clj"))
  ([conf-file]
   (let [{:keys [server user channels plugins]} (read-string (slurp conf-file))
         default-handlers [ cb-ping-pong ]
         handlers (concat default-handlers (require-plugins plugins))]
     (connect server user channels handlers))))
