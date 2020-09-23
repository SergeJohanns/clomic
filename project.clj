(defproject clomic "0.1.0-SNAPSHOT"
  :description "A telegram bot that automatically sends you new issues of webcomics you follow."
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-commons/clj-yaml "0.7.2"]]
  :profiles{:dev {:resource-paths ["test/resources"]}}
  :repl-options {:init-ns clomic.core})
