(ns bubblebot.core
  (:import (java.net Socket))
  (:require [clojure.java.io :as io]
            [bubbleirc.msg-builder :as cmd]
            [bubbleirc.msg-parser :refer [parse-message]]))

(defn- dodoseq
  "For each x, do all functions"
  [xs fs]
  (doseq [x xs] (doseq [f fs] (f x))))

(defn- msg-writer
  "Return a function to write a raw message (or a collection of messages)
   to given connection `conn`"
  [conn]
  (fn write [msg]
    (cond (coll? msg)
           (doseq [m msg] (write m))
          (and (string? msg) (not (empty? msg)))
           (binding [*out* (:out @conn)] (println msg)))))

(defn- cb-ping-pong [{:keys [command message]} _]
  (when (= command "PING") (cmd/pong message)))

(defn- create-connection
  "Create an IRC socket and return a map with reader and writer"
  [{:keys [host port]}]
  (let [socket (Socket. host port)]
    (ref {:in (io/reader socket) :out (io/writer socket)})))

(defn connect
  "Connect to server and register plugin handlers"
  [{:keys [server user channels] :as config} handlers]
  (let [conn (create-connection server)
        write (msg-writer conn)
        bot {:conn conn :config config}
        ; wrap handlers in `write`
        fns (map (fn [h] (comp write #(h % bot))) handlers)
        lines (map parse-message (line-seq (:in @conn)))]
    (.start (Thread. #(dodoseq lines fns)))
    (write (cmd/register-user user channels))
    conn))

(defn disconnect [conn]
  ((msg-writer conn) (cmd/quit)))

(defn require-plugins
  "Require plugins and return their message-handler functions"
  [plugins]
  (let [ps (map :ns (vals plugins))]
    (doseq [p ps] (require (symbol p)))
    (map (comp resolve symbol #(str % "/message-handler")) ps)))

(defn -main
  ([] (-main "config.clj"))
  ([conf-file]
   (let [config (read-string (slurp conf-file))
         handlers (conj (require-plugins (:plugins config)) cb-ping-pong)]
     (connect config handlers))))
