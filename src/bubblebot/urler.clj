(ns bubblebot.urler)

(defn listen
  "Given a parsed IRC line and a write function"
  [line write]
  (if (= (:cmd line) "PRIVMSG")
    (when-let [url (re-find #"(https?:\/\/)" (:msg line))]
      (println url))))
