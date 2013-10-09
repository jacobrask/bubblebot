(ns bubblebot.urler
  (:require [clojure.string :refer [trim]]
            [clj-http.client :as http-client]
            [net.cgrand.enlive-html :as html]
            [bubblebot.irc-cmd :as cmd]))

(defn find-title
  [h]
  (trim (html/text (first (html/select h [:title])))))

(defn find-url
  [str]
  (re-find #"https?:\/\/\S+" str))

(defn listen
  "Given a parsed IRC line and a write function"
  [line]
  (if (= (:cmd line) "PRIVMSG")
    (when-let [url (find-url (:msg line))]
      (let [rdr (java.io.StringReader. (:body (http-client/get url)))]
        (when-let [title (find-title (html/html-resource rdr))]
          (cmd/msg (:params line) (str "\u0002" title "\u0002 (" url ")")))))))
