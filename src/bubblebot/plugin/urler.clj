(ns bubblebot.plugin.urler
  "Prints the title of any URLs posted on IRC to the same channel/user.
  If a database is specified in the config file, also saves the URL data."
  (:require [clojure.string :as str]
            [com.ashafa.clutch :as couch]
            [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [bubblebot.irc-cmd :as cmd]))

(defn- save-url
  "Save URL in database"
  [data]
  (when-let [db (-> "config.clj" slurp read-string :plugins :urler :couch-url)]
    (couch/put-document db data)))

(defn- fetch-url-content
  "Get the HTML content from an URL as a string"
  [url]
  (try
    (let [{:keys [status body headers]}
           (http/get url {:headers {"Accept-Language" "sv,en,en-us"}
                          :ignore-unknown-host? true})]
      (when (re-find #"^text\/html" (get headers "content-type")) body))
    (catch Exception e (str "Couldn't GET " url ": " (.getMessage e)))))

(defn- normalize-whitespace [s]
  (str/trim (str/replace s #"\s+" " ")))

(defn find-title
  "Given a StringReader with HTML content, return the contents of the <title>
  element with normalized whitespace"
  [rdr]
  (-> (html/select (html/html-resource rdr) [:title]) first html/text normalize-whitespace))

(defn find-url
  [s]
  (re-find #"https?:\/\/[a-zA-Z0-9\-\._\?,\/\\\+%\$#=~]+" s))

(defn- title-from-url
  [url]
  (when-let [body (fetch-url-content url)]
    (find-title (java.io.StringReader. body))))

(defn- short-url
  [url]
  (if (> (count url) 25)
    (try
      (:body (http/get (str "http://tinyurl.com/api-create.php?url=" url)))
      (catch Exception e (str "Couldn't shorten " url ": " (.getMessage e))))
    url))

(defn message-handler
  [{[chan text] :params :keys [cmd nick]}]
  (when (= cmd "PRIVMSG")
    (when-let [url (find-url text)]
      (let [title (title-from-url url)]
        (save-url {:channel chan :url url
                   :title title  :text text
                   :nick nick    :date (System/currentTimeMillis)})
        (when-not (str/blank? title)
          (cmd/msg chan (str (cmd/bold title) " ("(short-url url)")")))))))
