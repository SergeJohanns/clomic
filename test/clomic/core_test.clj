(ns clomic.core-test
  (:require [clojure.test :refer :all]
            [clomic.core :refer :all]))

(def user 42)
(def subscribed-user 43)
(def unsubscribed-user 44)
(def real-feed "xkcd")
(def fake-feed "dckx")

(defn set-mock-feeds [f]
  (dosync (ref-set feeds {"xkcd" nil}))
  (f)
  (dosync (ref-set feeds {})))

(defn set-mock-users [f]
  (dosync (ref-set subscriptions {real-feed #{subscribed-user}}))
  (f))

(use-fixtures :each set-mock-feeds set-mock-users)

(deftest test-subscribe-user
  (testing "Subscribe a user to an existing feed"
    (is (= user (subscribe user real-feed)))
    (is (subscribed? user real-feed)))
  (testing "Fail to subscribe a user to a non-existing feed"
    (is (= nil (subscribe user fake-feed)))))

(deftest test-unsubscribe-user
  (testing "Remove a subscribed user from an existing feed"
    (is (subscribed? subscribed-user real-feed))
    (is (= subscribed-user (unsubscribe subscribed-user real-feed)))
    (is (not (subscribed? subscribed-user real-feed))))
  (testing "Fail to remove an unsubscribed user from an existing feed"
    (is (not (subscribed? unsubscribed-user real-feed)))
    (is (= nil (unsubscribe unsubscribed-user real-feed))))
  (testing "Fail to remove a user from a non-existing feed"
    (is (= nil (unsubscribe user fake-feed)))))
