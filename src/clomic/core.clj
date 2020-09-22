(ns clomic.core)

(def subscriptions (ref {}))
(def feeds {})

(defn feed? [feed]
  (contains? feeds feed))

(defn subscribed? [user-id feed]
  (and (feed? feed) ((@subscriptions feed) user-id)))

(defn update-subscriptions
  "Apply the function f to the set of subscriptions for the given feed."
  [feed f]
  (let [update-feed (fn [x g] (update x feed g))]
    (dosync (alter subscriptions update-feed f))))

(defn subscribe [user-id feed]
  (when (feed? feed)
    (if (contains? @subscriptions feed)
      (update-subscriptions feed (fn [y] (conj y user-id)))
      (update-subscriptions feed (fn [_] #{user-id})))
    user-id))

(defn unsubscribe [user-id feed]
  (when (subscribed? user-id feed)
    (update-subscriptions feed (fn [y] (disj y user-id)))
    user-id))
