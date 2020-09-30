(ns clomic.main
  (:require [clomic.core :as c]
            [clomic.persistence :as p]
            [clomic.bot :as b])
  (:gen-class))

(defmacro on-exit [f]
  `(.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] ~f))))

(defn -main [& args]
  (println "Preparing clomic...")
  (dosync (ref-set c/subscriptions (p/read-subscriptions)))
  (alter-var-root #'c/feeds (fn [_] (p/read-feeds)))
  (on-exit (p/write-subscriptions @c/subscriptions))
  (println "Starting bot...")
  (b/start-bot))
