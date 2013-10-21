(ns bubblebot.cmd-parser-test
  (:require [clojure.test :refer :all]
            [bubblebot.cmd-parser :refer :all]))

(deftest cmd-parser-test
  (testing "Get Clojure map from IRC message"
    (is (= (parse \! "!quote 123 456") {:bot-cmd "quote" :bot-args ["123" "456"]}))
    (is (= (parse \! "abcdef 123") nil))))
