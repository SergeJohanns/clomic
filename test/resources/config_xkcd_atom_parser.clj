(require '[feedparser-clj.core :refer [parse-feed]]
         '[clojure.zip :as zip]
         '[clojure.xml :as xml])
(import java.io.ByteArrayInputStream)

(defn parser [url]
  (let [zip-str
        (fn [s] (zip/xml-zip (xml/parse (ByteArrayInputStream. (.getBytes s)))))
        convert
        (fn [{timestamp :updated-date {summary :value} :description}]
          (let [[{fields :attrs}] (zip-str summary)]
            {:timestamp timestamp
             :image (fields :src)
             :alt-text (fields :alt)}))]
    (map convert ((parse-feed url) :entries))))
