(ns bubblebot.plugin.urler-test
  (:require [clojure.test :refer :all]
            [bubblebot.plugin.urler :refer :all]))

(def title-fixture "<html><head><title>  BUBBLEBOT \n</title></head>")

(deftest find-title-test
  (testing "Get title from HTML string"
    (is (= (find-title (java.io.StringReader. title-fixture)) "BUBBLEBOT"))))
