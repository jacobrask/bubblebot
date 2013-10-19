(ns bubblebot.plugin.urler-test
  (:require [clojure.test :refer :all]
            [bubblebot.plugin.urler :refer :all]))

(def title-fixtures [
  "<html><head><title>  BUBBLE BOT \n</title></head>"
  "<!doctype html><html><head>\n\t<title> \t BUBBLE BOT \n</title></head>"
  "<title>BUBBLE BOT"
  "<html><head>\n<title>\n  BUBBLE\n BOT \n</title></head>"])

(deftest find-title-test
  (testing "Get title from HTML string"
    (doseq [fix title-fixtures]
      (is (= (find-title fix) "BUBBLE BOT")))))
