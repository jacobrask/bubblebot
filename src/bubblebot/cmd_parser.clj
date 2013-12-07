(ns bubblebot.cmd-parser
  "Handle commands given to a bot in a message"
  (:require [clojure.string :refer [split join]]))

(defn parse
  "If msg is a bot command as determined by `pfx`, parse it"
  [pfx [one & text]]
  (when (= pfx one)
    (let [words (split (join text) #" ")]
      {:bot-cmd (first words) :bot-args (rest words)})))
