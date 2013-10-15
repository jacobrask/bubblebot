{:server {:host "irc.freenode.net" :port 6667}
 :user {:nick "bubblebot" :name "Too Much Bubble"}
 :channels ["###bubbletest"]
 :plugins {:logger {:ns bubblebot.plugin.logger}
           :urler {:ns bubblebot.plugin.urler
                   :couch-url "http://localhost:5984/urldb/"}}}
