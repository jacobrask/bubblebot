(ns bubblebot.plugin.logger)

(defn message-handler [{raw :raw}] (println raw))
