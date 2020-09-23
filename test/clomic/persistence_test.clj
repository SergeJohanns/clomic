(ns clomic.persistence-test
  (:require [clomic.persistence :as p]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import java.io.File
           java.io.FileNotFoundException))

(def config (io/resource "config.yaml"))
(def config-feeds #{:xkcd :not-xkcd})
(def parser-names ["xkcd_parser.clj" "not_xkcd_parser.clj"])
(def config-missing-parser (io/resource "config-missing.yaml"))

(defn delete-file-recursively
  "Delete `file` and, if it is a directory, delete all contained files and
  subdirectories as well. This function is not safe in general, because the JVM
  does not distinguish between files and symlinks, so it should only be called
  on files/directories that cannot contain symlinks."
  [file-name]
  (doall (map io/delete-file (reverse (file-seq (io/file file-name))))))

(defn change-root [f]
  (alter-var-root
   (var p/root)
   (fn [_] (str (System/getProperty "user.home") File/separator ".clomic-test")))
  (f)
  (delete-file-recursively p/feeds))

(defn make-parsers [f]
  (doseq [file-name parser-names
          :let [test-file (str p/feeds File/separator file-name)]]
    (io/make-parents test-file)
    (spit test-file (slurp (io/resource file-name))))
  (f))

(use-fixtures :once change-root make-parsers)

(deftest test-read-feeds
  (testing "Read feeds when all fields are provided."
    (is (= config-feeds (set (keys (p/read-feeds config))))))
  (testing "Fail to read feeds when a parser is missing"
    (is (thrown? FileNotFoundException
                 (set (keys (p/read-feeds config-missing-parser)))))))
