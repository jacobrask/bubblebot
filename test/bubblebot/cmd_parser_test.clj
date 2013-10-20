(ns bubblebot.cmd-parser-test
  (:require [clojure.test :refer :all]
            [bubblebot.cmd-parser :refer :all]))

(deftest cmd-parser-test
  (testing "Get Clojure map from IRC message"
    (is (= (parse \! "!quote 123 456") {:cmd "quote" :params "123 456"}))))
