(ns bubblebot.plugin.quotes
  (:require [clojure.string :refer [join trim]]
            [com.ashafa.clutch :as couch]
            [clucy.core :as clucy]
            [bubblebot.cmd-parser :as cmd-parser]
            [bubbleirc.msg-builder :as cmd]))

(def search-index (clucy/disk-index "search/quotes/"))

(def index-quote (partial clucy/add search-index))

(defn str->int [x]
  (try (Integer. (trim x)) (catch Exception _)))

(defn- quote-by-num
  "Save URL in database"
  [opts]
  (when-let [db (-> "config.clj" slurp read-string :plugins :quotes :couch-url)]
    (first (couch/get-view db "quote" :text-by-num (conj {:limit 1} opts)))))

(defn- quote-search [query] (clucy/search search-index query 1))

(defn- format-quote
  [k v]
  (str "[" (cmd/bold k) "] " v))

(defn add-quote
  [q]
  (let [n (inc (:key (quote-by-num {:descending true})))]
    (when-let [db (-> "config.clj" slurp read-string :plugins :quotes :couch-url)]
      (couch/put-document db (conj q {:num n}))
      (index-quote {:num n, :text (:text q)})
      (str "Quote " (cmd/bold n) " added."))))

(defn get-quote
  [which]
  (cond
    (= :random which)
      (let [last-num (:key (quote-by-num {:descending true}))
            q-num (inc (rand-int last-num))]
        (when-let [q (quote-by-num {:key q-num})]
          (format-quote (:key q) (:value q))))
    (= :last which)
      (when-let [q (quote-by-num {:descending true})]
        (format-quote (:key q) (:value q)))
    (= 1 (count which))
     (let [word (first which)]
       (if-let [q-num (str->int word)]
        ; Quote search is numeric
         (if-let [q (quote-by-num {:key q-num})]
           (format-quote (:key q) (:value q))
           (str "No such quote " (cmd/bold q-num)))
         (if-let [q (first (quote-search word))]
           (format-quote (:num q) (:text q))
           (str "No matches for \"" word \"))))
    :else
       (if-let [q (first (quote-search (join " " which)))]
         (format-quote (:num q) (:text q))
         (str "No matches for \"" (join " " which) \"))))

(defn message-handler
  [{[chan] :middle, text :trailing, :keys [nick command]}]
  (when (= "PRIVMSG" command)
    (when-let [{:keys [bot-cmd bot-args]} (cmd-parser/parse \! text)]
      (cmd/msg chan
        (cond
          (= "q" bot-cmd)
           (if (= 0 (count bot-args))
               (get-quote :random)
               (get-quote bot-args))
          (= "qlast" bot-cmd)
           (get-quote :last)
          (= "qlist" bot-cmd)
           "List quotes"
          (= "addquote" bot-cmd)
           (add-quote {:nick nick :date (System/currentTimeMillis)
                       :text (join " " bot-args)}))))))
