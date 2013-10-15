(ns bubblebot.plugin.logger)

(defn message-handler
  [line]
  (println (:raw line)))
