(ns bubblebot.urler
  (:require [clojure.string :refer [trim]]
            [clj-http.client :as http-client]
            [net.cgrand.enlive-html :as html]
            [bubblebot.irc-cmd :as cmd]))

(defn fetch-url-content
  [url]
  (:body (http-client/get url {:headers {"Accept-Language" "sv,en,en-us"}
                               :ignore-unknown-host? true
                               :throw-exceptions false})))

(defn find-title
  [rdr]
  (trim (html/text (first (html/select rdr [:title])))))

(defn find-url
  [s]
  (re-find #"https?:\/\/\S+" s))

(defn title-from-url
  [url]
  (when-let [body (fetch-url-content url)]
    (find-title (html/html-resource (java.io.StringReader. body)))))

(defn bold [s] (str "\u0002" s "\u0002"))

(defn short-url
  [url]
  (if (> (count url) 25)
    (:body (http-client/get (str "http://tinyurl.com/api-create.php?url=" url)))
    url))

(defn listen
  [line]
  (if (= (:cmd line) "PRIVMSG")
    (when-let [url (find-url (:msg line))]
      (when-let [title (title-from-url url)]
        (cmd/msg (:params line) (str (bold title) " (" (short-url url) ")"))))))
