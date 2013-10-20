(ns bubbleirc.parser-test
  (:require [clojure.test :refer :all]
            [bubbleirc.msg-parser :refer :all]))

(deftest parse-message-test
  (testing "Parse message"
    (is (= (parse-message ":irc.example.net NOTICE * :*** Looking up your hostname...")
           {:servername "irc.example.net"
            :command "NOTICE"
            :middle ["*"]
            :trailing "*** Looking up your hostname..."
            :message ":irc.example.net NOTICE * :*** Looking up your hostname..."}))
    (is (= (parse-message "PING :irc.example.net")
           {:command "PING"
            :trailing "irc.example.net"
            :message "PING :irc.example.net"}))
    (is (= (parse-message ":irc.example.net 366 bubblebot #example :End of /NAMES list.")
           {:servername "irc.example.net"
            :command "366"
            :middle ["bubblebot" "#example"]
            :trailing "End of /NAMES list."
            :message ":irc.example.net 366 bubblebot #example :End of /NAMES list."}))
    (is (= (parse-message ":nick!~user@user.example.net MODE #example +v testuser")
           {:nick "nick" :user "~user" :host "user.example.net"
            :command "MODE"
            :middle ["#example" "+v" "testuser"]
            :message ":nick!~user@user.example.net MODE #example +v testuser"}))
    (is (= (parse-message ":nick!~user@user.example.net PRIVMSG #example :Lorem ipsum dolor sit amet :) PRIVMSG :P")
           {:nick "nick" :user "~user" :host "user.example.net"
            :command "PRIVMSG"
            :middle ["#example"]
            :trailing "Lorem ipsum dolor sit amet :) PRIVMSG :P"
            :message ":nick!~user@user.example.net PRIVMSG #example :Lorem ipsum dolor sit amet :) PRIVMSG :P"}))
    (is (= (parse-message ":bubblebot!~bubblebot@example.com JOIN #example")
           {:nick "bubblebot" :user "~bubblebot" :host "example.com"
            :command "JOIN"
            :middle ["#example"]
            :message ":bubblebot!~bubblebot@example.com JOIN #example"}))))
