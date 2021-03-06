(ns clomic.persistence-test
  (:require [clomic.persistence :as p]
            [clojure.set :as s]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import java.io.File
           java.io.FileNotFoundException))

(def config (io/resource "config.yaml"))
(def config-feeds #{:xkcd :not-xkcd})
(def test-subscriptions-file (io/resource "subscriptions.clj"))
(def missing-subscriptions-file "test_subscriptions.clj")
(defn missing-subscriptions [] ;; Not a var, since p/root may change.
  (str p/root File/separator missing-subscriptions-file))
(def test-subscriptions {:xkcd #{0 42} :not-xkcd #{5 41}})
(def other-test-subscriptions {:other-xkcd #{1 41} :different-xkcd #{2 40}})
(def test-timestamp-file (io/resource "timestamp.clj"))
(def test-timestamp #inst "2020-01-02T03:04:05Z")
(def other-test-timestamp #inst "2020-05-04T03:02:01Z")
(def missing-timestamp-file "test_timestamp.clj")
(defn missing-timestamp []
  (str p/root File/separator missing-timestamp-file))

(def parser-names [["xkcd_parser.clj" "xkcd_parser.clj"]
                   ["not_xkcd_parser.clj" "not_xkcd_parser.clj"]
                   ["dummy_config.clj" "dummy.clj"]])
(def config-missing-parser (io/resource "config-missing.yaml"))
(def xkcd-atom-feed (io/resource "xkcd-atom.xml"))
(def xkcd-atom-feed-result
  (edn/read-string (slurp (io/resource "xkcd_atom_result.clj"))))
(def resource-xkcd-parser (atom nil))
(def config-xkcd-parser (atom nil))
;; Both dummy parsers have the same name, to test precedence.
(def resource-dummy-parser (atom nil))
(def config-dummy-parser (atom nil))
(def config-dummy-result "Config dummy")

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
  (alter-var-root (var p/parsers) (fn [_] (str p/root File/separator "parsers")))
  (alter-var-root (var p/subscriptions)
                  (fn [_] (str p/root File/separator "subscriptions.clj")))
  (alter-var-root (var p/timestamp)
                  (fn [_] (str p/root File/separator "timestamp.clj")))
  (f)
  (delete-file-recursively p/parsers))

(defn make-parsers [f]
  (doseq [[file-name new-name] parser-names
          :let [test-file (str p/parsers File/separator new-name)]]
    (io/make-parents test-file)
    (spit test-file (slurp (io/resource file-name))))
  (f))

(defn prepare-parsers [f]
  (reset! resource-xkcd-parser (p/resolve-parser "resource_xkcd_atom_parser"))
  (reset! config-xkcd-parser (p/resolve-parser "config_xkcd_atom_parser"))
  (reset! resource-dummy-parser (p/resolve-parser "dummy_resource"))
  (reset! config-dummy-parser (p/resolve-parser "dummy_config"))
  (f))

(use-fixtures :once change-root make-parsers prepare-parsers)

(defn prepare-subscription-file [f]
  (f)
  (if (.exists (io/file (missing-subscriptions)))
    (io/delete-file (missing-subscriptions))))

(defn prepare-timestamp-file [f]
  (f)
  (if (.exists (io/file (missing-timestamp)))
    (io/delete-file (missing-timestamp))))

(use-fixtures :each prepare-subscription-file prepare-timestamp-file)

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

(deftest test-read-timestamp
  (testing "Is 70/1/1 when the file is missing."
    (is (= #inst "1970-01-01T00:00:00Z" (p/read-timestamp (missing-timestamp)))))
  (testing "Read timestamp from persistent storage."
    (is (= test-timestamp (p/read-timestamp test-timestamp-file)))))

(deftest test-write-timestamp
  (testing "Write timestamp to persistent storage."
    (is (= #inst "1970-01-01T00:00:00Z" (p/read-timestamp (missing-timestamp))))
    (p/write-timestamp (missing-timestamp) other-test-timestamp)
    (is (= other-test-timestamp (p/read-timestamp (missing-timestamp))))))

(deftest test-resolve-parser
  (testing "Prioritise config parsers over resource parsers."
    (is (= config-dummy-result (@config-dummy-parser xkcd-atom-feed)))))

(defn all-submaps? [as bs]
  (every? (fn [[a b]] (s/subset? (set a) (set b))) (map vector as bs)))

(deftest test-parser
  (testing "Load a parser from a resource and correctly parse an atom feed."
    (is (all-submaps? xkcd-atom-feed-result (@resource-xkcd-parser xkcd-atom-feed))))
  (testing "Load a parser from a config file and correctly parse an atom feed."
    (is (all-submaps? xkcd-atom-feed-result (@config-xkcd-parser xkcd-atom-feed)))))
