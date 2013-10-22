(ns bubblebot.plugin.logger)

(defn message-handler [{:keys [message]} _] (println message))
