(ns bubblebot.plugin.quotes
  (:require [clojure.string :refer [join trim]]
            [com.ashafa.clutch :as couch]
            [bubblebot.cmd-parser :as cmd-parser]
            [bubbleirc.msg-builder :as cmd]))

(defn str->int [x]
  (try (Integer. (trim x)) (catch Exception _)))

(defn- quote-by-num
  "Save URL in database"
  [opts]
  (when-let [db (-> "config.clj" slurp read-string :plugins :quotes :couch-url)]
    (first (couch/get-view db "quote" :text-by-num (conj {:limit 1} opts)))))

(defn- format-quote
  [k v]
  (str "[" (cmd/bold k) "] " v))

(defn add-quote
  [q]
  (let [n (inc (:key (quote-by-num {:descending true})))]
    (when-let [db (-> "config.clj" slurp read-string :plugins :quotes :couch-url)]
      (couch/put-document db (conj q {:num n}))
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
     (if-let [q-num (str->int (first which))]
       (if-let [q (quote-by-num {:key q-num})]
         (format-quote (:key q) (:value q))
         (str "No such quote: " (cmd/bold q-num)))
       (str "No matches for \"" (join " " which) \"))
    :else
       (str "No matches for \"" (join " " which) \")))

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
