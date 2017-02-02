(ns web.data-store.watches
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [clojure-watch.core :as fs-watch]))

(defn get-path
  [file]
  (.getPath file))

(defn bootstrap
  [chan path]
  (async/onto-chan chan
                   (map (fn [file] [:create (get-path file)])
                        (file-seq (io/as-file path)))
                   false))

(defn callback
  [chan event path]
  (async/put! chan [event path]))

(defn watch-spec
  [chan path]
  {:path        path
   :event-types [:create :modify :delete]
   :bootstrap   (partial bootstrap chan)
   :callback    (partial callback chan)
   :options     {:recursive true}})

(defn start-watch
  [watch-specs]
  (fs-watch/start-watch watch-specs))

