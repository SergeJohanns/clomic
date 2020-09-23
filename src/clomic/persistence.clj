(ns clomic.persistence
  (:require [clojure.java.io :as io]
            [clj-yaml.core :refer [parse-string]])
  (:import java.io.File
           java.io.FileNotFoundException
           javax.naming.ConfigurationException))

(def root (System/getProperty "user.home"))
(def config (str root File/separator "config.yaml"))
(def feeds (str root File/separator "feeds"))

(defn resolve-parser
  "Construct the parser function corresponding to the `feed`."
  [feed]
  (let [file-path (str feeds File/separator feed ".clj")]
    (if (.exists (io/file file-path))
      (read-string (slurp file-path))
      (throw (FileNotFoundException.
              (str "Could not find the feed parser file '" file-path "'"))))))

(defn read-feeds
  "Read the content feeds and their respective parser functions into a map.
  Optionally take the path of a `config-file` to overwrite the default."
  ([] (read-feeds config))
  ([config-file]
   (if (not (.exists (io/file config-file)))
     (throw (FileNotFoundException.
             (str "Could not find the config file '" config-file "'"))))
   (let [result (parse-string (slurp config-file))]
     (if (not (contains? result :feeds))
       (throw (ConfigurationException.
               "The config file is missing the :feeds field."))
       (into {} (for [[k {url :url parser :parser}] (result :feeds)]
                  [k {:url url :parser (resolve-parser parser)}]))))))
