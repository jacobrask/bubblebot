(ns bubblebot.plugin.urler
  (:require [clojure.string :as string]
            [com.ashafa.clutch :as couch]
            [clj-http.client :as http-client]
            [net.cgrand.enlive-html :as html]
            [bubblebot.irc-cmd :as cmd]))

(defn- save-url
  "Save URL in database"
  [data]
  (when-let [db (:couch-url (:urler (:plugins (read-string (slurp "config.clj")))))]
    (couch/put-document db data)))

(defn- fetch-url-content
  "Get the HTML content from an URL as a string"
  [url]
  (try
    (:body (http-client/get url {:headers {"Accept-Language" "sv,en,en-us"}
                                 :ignore-unknown-host? true}))
    (catch Exception e (string/join "Couldn't GET" url ":" (.getMessage e)))))


(defn- normalize-whitespace [s]
  (string/trim (string/replace s #"\s+" " ")))

(defn- find-title
  "Given a StringReader with HTML content, return the contents of the <title>
  element with normalized whitespace"
  [rdr]
  (normalize-whitespace (html/text (first (html/select rdr [:title])))))

(defn- find-url
  [s]
  (re-find #"https?:\/\/[a-zA-Z0-9\-\._\?,\/\\\+%\$#=~]+" s))

(defn- title-from-url
  [url]
  (when-let [body (fetch-url-content url)]
    (find-title (html/html-resource (java.io.StringReader. body)))))

(defn- bold [s] (str "\u0002" s "\u0002"))

(defn- short-url
  [url]
  (if (> (count url) 25)
    (try
      (:body (http-client/get (str "http://tinyurl.com/api-create.php?url=" url)))
      (catch Exception e (string/join "Couldn't shorten" url ":" (.getMessage e))))
    url))

(defn message-handler
  [line]
  (when (= (:cmd line) "PRIVMSG")
    (let [chan (first (:params line))
          text (fnext (:params line))]
      (when-let [url (find-url text)]
        (when-let [title (title-from-url url)]
          (when (not (string/blank? title))
            (save-url {:channel chan
                       :url url
                       :title title
                       :text text
                       :nick (:nick line)
                       :date (System/currentTimeMillis)})
            (cmd/msg chan (str (bold title) " (" (short-url url) ")"))))))))
