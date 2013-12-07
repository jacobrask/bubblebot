(ns bubblebot.plugin.logger)

(defn message-handler [{msg :message} _] (println msg))
