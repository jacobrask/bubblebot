;; Copyright (c) 2011 irclj authors <https://github.com/flatland/irclj>
;;
;; This is a lightweight parser that takes an IRC message and generates
;; a Clojure map of information from that message. One piece of terminology
;; to keep in mind is 'line sequence'. In this context, it means the IRC
;; message broken up at spaces.
(ns bubblebot.msg-parser
  "IRC messages -> Clojure maps"
  (:require [clojure.string :as str]))

;; Prefixes are optional, so this might return nil. We'll use the
;; result of this to know where to parse other things in the message
;; later.
(defn extract-prefix
  "Takes a line sequence and if the first element is a str
   beginning with ':', return it minus the ':'."
  [line-s]
  (when (= \: (ffirst line-s)) (str/join (rest (first line-s)))))

(defn parse-prefix
  "If a prefix is present in the message, parse it and return
   a map of :nick, :user, and :host."
  [line-s]
  (when-let [prefix (extract-prefix line-s)]
    (zipmap [:nick :user :host] (str/split prefix #"!|@"))))

;; If there is a prefix, we know we need to drop two elements from
;; the message in order to get to the parameters. The approach we
;; take to parsing the parameters is to make it all a str, split
;; on the first ':' character, and then split the first half of the
;; result on spaces and conjoin the second half to that list. The
;; reason this is necessary is because the last parameter in an IRC
;; message can contain spaces.
(defn parse-params
  "Parse the parameters of a message. prefix is a true or false
   value."
  [line-s prefix]
  (let [[single multi] (str/split
                        (str/join
                         " "
                         (if prefix
                           (drop 2 line-s)
                           (rest line-s)))
                        #":"
                        2)
        split-single (str/split single #" ")]
    (if multi
      (if (seq single)
        (conj split-single multi)
        [multi])
      split-single)))

(defn parse
  "Takes a raw message from IRC and turns it into a Clojure map.
   This map will contain :cmd, :params, :raw keys. If the message
   begins with a prefix, it will be parsed and :nick, :user, and :host
   keys will also be in the resulting map."
  [line]
  (let [line-s (str/split line #" ")
        prefix (parse-prefix line-s)]
    (into
     {:cmd (if prefix (second line-s) (first line-s))
      :params (parse-params line-s prefix)
      :raw line}
     prefix)))