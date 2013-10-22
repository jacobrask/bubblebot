(ns bubblebot.plugin.urler
  "Prints the title of any URLs posted on IRC to the same channel/user.
  If a database is specified in the config file, also saves the URL data."
  (:require [clojure.string :as str]
            [com.ashafa.clutch :as couch]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [bubbleirc.msg-builder :as cmd]))

(defn- save-url
  [db url]
  (couch/put-document db url))

(defn- fetch-url-content
  "Get the HTML content from an URL as a string. Returns nil for valid but
  non-HTML responses, throws otherwise."
  [url]
  (let [{:keys [status body headers]}
        (http/get url {:headers {"Accept-Language" "en,en-us"
                                 "User-Agent" "Mozilla/5.0 (bubble url-title)"}})]
    (when (re-find #"^text\/html" (get headers "content-type")) body)))

(defn- normalize-whitespace [s]
  (str/trim (str/replace s #"\s+" " ")))

(defn find-title
  "Given a string of HTML content, return the contents of the <title> element
  with normalized whitespace"
  [content]
  (-> (html/select (html/html-resource (java.io.StringReader. content)) [:title]) first html/text normalize-whitespace))

(defn find-url
  [s]
  (re-find #"https?:\/\/[a-zA-Z0-9\-\._\?,\/\\\+%\$#=~]+" s))

(defn- title-from-url
  [url]
  (when-let [body (fetch-url-content url)]
    (find-title body)))

(defn- short-url
  [url]
  (if (> (count url) 40)
    (try
      (:body (http/get (str "http://is.gd/create.php?format=simple&url=" url)))
      (catch Exception ex (log/info (.getMessage ex))))
    url))

(defn message-handler
  [{[chan] :middle, cmd :command, nick :nick, text :trailing} {cfg :config}]
  (when (= "PRIVMSG" cmd)
    (when-let [url (find-url text)]
      (try
        (let [title (title-from-url url),
              db (-> cfg :plugins :urler :couch-url)]
          (save-url db {:channel chan :url url
                        :title title  :text text
                        :nick nick    :date (System/currentTimeMillis)})
          (cmd/msg chan
            (if (str/blank? title)
              (when (> (count url) 40) (short-url url))
              (str (cmd/bold title) " ("(short-url url)")"))))
        (catch Exception ex (log/info (.getMessage ex) url))))))
