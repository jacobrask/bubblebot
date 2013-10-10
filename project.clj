(defproject bubblebot "0.1.0-SNAPSHOT"
  :description "IRC bot"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha5"]
                 [org.clojure/data.json "0.2.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [clj-http "0.7.7"]
                 [enlive "1.1.4"]]
  :main bubblebot.core
  :profiles {:uberjar {:aot :all}})
