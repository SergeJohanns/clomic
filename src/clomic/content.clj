(ns clomic.content
  (:require [clomic.core :as c]
            [clomic.persistence :as p]
            [clomic.bot :as b]
            [clojure.core.async :refer [go-loop <! timeout]])
  (:import (java.time LocalDateTime ZoneId)
           java.util.Date))

(def refresh-timeout 600000)

(defn get-content [feeds last-update]
  (into {}
        (for [[feed {parser :parser url :url}] feeds]
          [feed
           (filter (fn [{timestamp :timestamp}] (.after timestamp last-update))
                   (parser url))])))

(defn update-content []
  (let [time (p/read-timestamp)
        content (get-content c/feeds time)
        now (-> (LocalDateTime/now)
                (.atZone (ZoneId/systemDefault))
                (.toInstant)
                (Date/from))]
    (p/write-timestamp now)
    (doseq [[feed users] @c/subscriptions user users]
      (doall (map #(b/send-content % user) (content feed))))))

(defn update-cycle []
  (go-loop []
    (do
      (update-content)
      (<! (timeout refresh-timeout))
      (recur))))
