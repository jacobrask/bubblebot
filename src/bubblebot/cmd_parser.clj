(ns bubblebot.cmd-parser
  (:require [clojure.string :refer [split join]]))

(defn parse
  "If msg is a bot command, parse it and return"
  [pfx [one & text]]
  (when (= pfx one)
    (let [words (split (join text) #" ")]
      {:bot-cmd (first words) :bot-args (rest words)})))
