(defproject clomic "0.1.0-SNAPSHOT"
  :description "A telegram bot that automatically sends you new issues of webcomics you follow."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-commons/clj-yaml "0.7.2"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]]
  :profiles{:dev {:resource-paths ["test/resources"]}}
  :repl-options {:init-ns clomic.core})
