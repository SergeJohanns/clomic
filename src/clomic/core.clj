(ns clomic.core)

(def subscriptions (ref {}))
(def feeds (ref {}))

(defn feed? [feed]
  (contains? @feeds feed))

(defn subscribed? [user-id feed]
  (and (feed? feed) (contains? (@subscriptions feed) user-id)))

(defn update-subscriptions [feed f]
  (let [update-feed (fn [x g] (update x feed g))]
    (dosync (alter subscriptions update-feed f))))

(defn subscribe [user-id feed]
  (if (feed? feed)
    (do (if (contains? @subscriptions feed)
          (update-subscriptions feed (fn [y] (conj y user-id)))
          (update-subscriptions feed (fn [_] #{user-id})))
        user-id)
    nil))

(defn unsubscribe [user-id feed]
  (if (subscribed? user-id feed)
    (do (update-subscriptions feed (fn [y] (disj y user-id)))
        user-id)
    nil))
