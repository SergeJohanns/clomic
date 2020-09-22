(ns clomic.core-test
  (:require [clojure.test :refer :all]
            [clomic.core :refer :all]))

(defn set-mock-feeds [f]
  (dosync (ref-set feeds {"xkcd" nil}))
  (f)
  (dosync (ref-set feeds {})))

(use-fixtures :each set-mock-feeds)

(deftest test-subscribe-user
  (testing "Subscribe a user to an existing feed"
    (is (= 0 (subscribe 0 "xkcd")))
    (is (subscribed? 0 "xkcd")))
  (testing "Fail to subscribe a user to a non-existing feed"
    (is (= nil (subscribe 0 "this-is-not-a-real-feed")))))

(deftest test-unsubscribe-user
  (subscribe 42 "xkcd")
  (testing "Remove a subscribed user from an existing feed"
    (is (subscribed? 42 "xkcd"))
    (is (= 42 (unsubscribe 42 "xkcd")))
    (is (not (subscribed? 42 "xkcd"))))
  (testing "Fail to remove an unsubscribed user from an existing feed"
    (is (not (subscribed? 41 "xkcd")))
    (is (= nil (unsubscribe 41 "xkcd"))))
  (testing "Fail to remove a user from a non-existing feed"
    (is (= nil (unsubscribe 42 "this-is-not-a-real-feed")))))
