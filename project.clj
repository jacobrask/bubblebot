(defproject bubblebot "0.1.0-SNAPSHOT"
  :description "IRC bot"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.ashafa/clutch "0.4.0-RC1"]
                 [clucy "0.4.0"]
                 [clj-http "0.7.7"]
                 [enlive "1.1.4"]]
  :main bubblebot.core
  :profiles {:uberjar {:aot :all}})
