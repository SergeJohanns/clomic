(ns clomic.persistence
  (:require [clojure.java.io :as io]
            [clj-yaml.core :refer [parse-string]]
            [clojure.edn :as edn])
  (:import java.io.File
           java.io.FileNotFoundException
           javax.naming.ConfigurationException))

(def parser-function 'parser)
(def root (str (System/getProperty "user.home")
               File/separator ".config"
               File/separator "clomic"))
(def config (str root File/separator "config.yaml"))
(def parsers (str root File/separator "parsers"))
(def subscriptions (str root File/separator "subscriptions.clj"))
(def timestamp (str root File/separator "timestamp.clj"))

(defn resolve-parser
  "Construct the parser function corresponding to the `feed`. Prioritise user
  config files over builtin resources."
  [feed]
  (let [name (str feed ".clj") path (str parsers File/separator name)]
    (cond
      (.exists (io/file path)) (load-file path)
      (io/resource name) (load-string (slurp (io/resource name)))
      :else (throw (FileNotFoundException.
             (str "Could not find the feed parser file '" path "'"))))
    ;; Parser function has been set by the parser library
    ;; Unmap it so it doesn't override the next library
    (let [parser-instance (resolve parser-function)]
      (ns-unmap *ns* parser-function)
      parser-instance)))

(defn read-feeds
  "Read the content feeds and their respective parser functions into a map.
  Optionally take the path of a `config-file` to override the default."
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

(defn read-subscriptions
  "Read subscriptions from persistent storage. Optionally take the path of a
  `subscriptions-file` to override the default."
  ([] (read-subscriptions subscriptions))
  ([subscriptions-file]
   (if (.exists (io/file subscriptions-file))
     (edn/read-string (slurp subscriptions-file))
     {})))

(defn write-subscriptions
  "Write subscriptions to persistent storage. Optionally take the path of a
  `subscriptions-file` to override the default."
  ([data] (write-subscriptions subscriptions data))
  ([subscriptions-file data]
   (io/make-parents subscriptions-file)
   (spit subscriptions-file (pr-str data))))

(defn read-timestamp
  "Read the time of the last content update from persistent storage. Optionally
  take the path of a `timestamp-file` to override the default."
  ([] (read-timestamp timestamp))
  ([timestamp-file]
   (if (.exists (io/file timestamp-file))
     (edn/read-string (slurp timestamp-file))
     #inst "1970-01-01T00:00:00Z")))

(defn write-timestamp
  "Write the given `time` to persistent storage. Optionally take the path of a
  `timestamp-file` to override the default."
  ([time] (write-timestamp timestamp time))
  ([timestamp-file time] (spit timestamp-file (pr-str time))))
