(ns bubblebot.irc-cmd-test
  (:require [clojure.test :refer :all]
            [bubblebot.irc-cmd :as cmd]))

(deftest user-test
  (testing "Generate USER string"
    (let [u (cmd/user "tester" "Mr. Test")]
      (is (= u "USER tester 0 * :Mr. Test")))))
