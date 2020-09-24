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
(def test-subscriptions-file (io/resource "subscriptions.clj"))
(def missing-subscriptions-file "test_subscriptions.clj")
(defn missing-subscriptions [] ;; Not a var, since p/root may change.
  (str p/root File/separator missing-subscriptions-file))
(def test-subscriptions {:xkcd #{0 42} :not-xkcd #{5 41}})
(def other-test-subscriptions {:other-xkcd #{1 41} :different-xkcd #{2 40}})

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

(defn prepare-subscription-file [f]
  (f)
  (if (.exists (io/file (missing-subscriptions)))
    (io/delete-file (missing-subscriptions))))

(use-fixtures :each prepare-subscription-file)

(deftest test-read-feeds
  (testing "Read feeds when all fields are provided."
    (is (= config-feeds (set (keys (p/read-feeds config))))))
  (testing "Fail to read feeds when a parser is missing"
    (is (thrown? FileNotFoundException
                 (set (keys (p/read-feeds config-missing-parser)))))))

(deftest test-read-subscriptions
  (testing "Is empty when the file is missing."
    (is (= {} (p/read-subscriptions (missing-subscriptions)))))
  (testing "Read subscriptions from persistent storage."
    (is (= test-subscriptions (p/read-subscriptions test-subscriptions-file)))))

(deftest test-write-subscriptions
  (testing "Write subscriptions to persistent storage."
    (is (= {} (p/read-subscriptions (missing-subscriptions))))
    (p/write-subscriptions (missing-subscriptions) other-test-subscriptions)
    (is (= other-test-subscriptions (p/read-subscriptions (missing-subscriptions))))))
