(ns bubblebot.plugin.quotes
  (:require [clojure.string :as str]
            [com.ashafa.clutch :as couch]
            [clucy.core :as clucy]
            [bubblebot.cmd-parser :as cmd-parser]
            [bubbleirc.msg-builder :as cmd]))

(defn str->int [x]
  (try (Integer. (str/trim x)) (catch Exception _)))

(def sentence (partial str/join " "))

(def search-index (clucy/disk-index "search/quotes/"))

(defn index-all-quotes
  "Add all quotes in `db` to local search index"
  [db]
  (doseq [q (couch/get-view db "quote" :text-by-num)]
    (clucy/add search-index {:num (:key q) :text (:value q)})))

(defn quote-by-num
  "Save URL in database"
  [db opts]
  (first (couch/get-view db "quote" :text-by-num (conj {:limit 1} opts))))

(defn quote-search [query]
  (try
    (clucy/search search-index query 99)
    (catch Exception _)))

(defn- format-quote
  [n text]
  (str "[" (cmd/bold n) "] " (str/replace text #"[\n\r]" " ")))

(defn add-quote
  [db q]
  (let [n (inc (:key (quote-by-num {:descending true})))]
    (couch/put-document db (conj q {:num n}))
    (clucy/add search-index {:num n, :text (:text q)})
    (str "Quote " (cmd/bold n) " added.")))

(defn get-quote
  [db which]
  (let [by-num (partial quote-by-num db)]
    (cond
      (= :random which)
        (let [last-num (:key (by-num {:descending true}))
              q-num (inc (rand-int last-num))]
          (when-let [q (by-num {:key q-num})]
            (format-quote (:key q) (:value q))))
      (= :last which)
        (when-let [q (by-num {:descending true})]
          (format-quote (:key q) (:value q)))
      (= 1 (count which))
       (let [word (first which)]
         (if-let [q-num (str->int word)]
           ; Quote search is numeric
           (if-let [q (by-num {:key q-num})]
             (format-quote (:key q) (:value q))
             (str "No such quote " (cmd/bold q-num)))
           (if-let [qs (seq (quote-search word))]
             (let [q (rand-nth qs)] (format-quote (:num q) (:text q)))
             (str "No matches for \"" word \"))))
      :else
         (if-let [qs (seq (quote-search (sentence which)))]
           (let [q (rand-nth qs)] (format-quote (:num q) (:text q)))
           (str "No matches for \"" (sentence which) \")))))

(defn message-handler
  [{[chan] :middle, text :trailing, :keys [nick command]} {cfg :config}]
  (when (= "PRIVMSG" command)
    (when-let [{:keys [bot-cmd bot-args]} (cmd-parser/parse \! text)]
      (let [db (-> cfg :plugins :quotes :couch-url)
            get-q (partial get-quote db)]
        (cmd/msg chan
          (cond
            (= "q" bot-cmd)
             (if (= 0 (count bot-args))
               (get-q :random)
               (get-q bot-args))
            (= "qlast" bot-cmd)
             (get-q :last)
            (= "qlist" bot-cmd)
             "List quotes"
            (= "addquote" bot-cmd)
             (add-quote db {:adder nick :date (System/currentTimeMillis)
                            :text (sentence bot-args)})))))))
