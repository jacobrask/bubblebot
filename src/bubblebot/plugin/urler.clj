(ns bubblebot.plugin.urler
  "Prints the title of any URLs posted on IRC to the same channel/user.
  If a database is specified in the config file, also saves the URL data."
  (:require [clojure.string :as str]
            [com.ashafa.clutch :as couch]
            [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [bubblebot.msg-builder :as cmd]))

(defn- save-url
  "Save URL in database"
  [data]
  (when-let [db (-> "config.clj" slurp read-string :plugins :urler :couch-url)]
    (couch/put-document db data)))

(defn- fetch-url-content
  "Get the HTML content from an URL as a string. Returns nil for valid but
  non-HTML responses, throws otherwise."
  [url]
  (let [{:keys [status body headers]}
         (http/get url {:headers {"Accept-Language" "en,en-us"}})]
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
  (if (> (count url) 25)
    (try
      (:body (http/get (str "http://tinyurl.com/api-create.php?url=" url)))
      (catch Exception e (prn (str "Couldn't shorten " url ": " (.getMessage e)))))
    url))

(defn message-handler
  [{[chan] :middle, cmd :command, nick :nick, text :trailing}]
  (when (= cmd "PRIVMSG")
    (when-let [url (find-url text)]
      (try
        (let [title (title-from-url url)]
          (save-url {:channel chan :url url
                     :title title  :text text
                     :nick nick    :date (System/currentTimeMillis)})
          (if (str/blank? title)
            (when (> (count url) 25) (cmd/msg chan (short-url url)))
            (cmd/msg chan (str (cmd/bold title) " ("(short-url url)")"))))
        (catch Exception e (prn (str "Couldn't fetch " url ": " (.getMessage e))))))))
