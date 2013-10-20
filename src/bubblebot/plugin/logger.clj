(ns bubblebot.plugin.logger)

(defn message-handler [{:keys [message]}] (println message))
