(ns clomic.bot
  (:require [clomic.core :as c]
            [clojure.string :as s]
            [clojure.core.async :refer [<!!]]
            [clojure.java.io :as io]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as a]
            [environ.core :refer [env]]))

(def token (env :bot-token))

(def start-response (slurp (io/resource "start-response.md")))
(def help-response (slurp (io/resource "help-response.md")))
(def subscribe-response (slurp (io/resource "subscribe-response.md")))
(def unsubscribe-response (slurp (io/resource "unsubscribe-response.md")))
(def not-a-feed-response (slurp (io/resource "not-a-feed-response.md")))
(def no-feeds (slurp (io/resource "no-feeds.md")))

(defn format-feeds [feeds]
  (if (= {} feeds)
    no-feeds
    (s/join \newline
            (for [feed-name (sort (keys feeds))]
              (str "/subscribe " (name feed-name))))))

(let [send (fn [id m] (a/send-text token id {:parse_mode "Markdown"} m))]
  (h/defhandler telegram-bot
    (h/command "start" {{id :id} :chat}
               (send id start-response))

    (h/command "help" {{id :id} :chat}
               (send id help-response))

    (h/command "feeds" {{id :id} :chat}
               (println c/feeds)
               (send id (format-feeds c/feeds)))

    (h/command "subscribe" {{id :id} :chat text :text}
               (let [feed (keyword (second (s/split text #" " 2)))]
                 (if (c/subscribe id feed)
                   (send id (format subscribe-response feed))
                   (send id (format not-a-feed-response feed)))))

    (h/command "unsubscribe" {{id :id} :chat text :text}
               (let [feed (keyword (second (s/split text #" " 2)))]
                 (if (c/unsubscribe id feed)
                   (send id (format unsubscribe-response feed))
                   (send id (format not-a-feed-response feed)))))))

(defn start-bot []
  (<!! (p/start token telegram-bot)))
