(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [hiccup.core :as hiccup]))

(defn slurp-reagent!
  "Slurp the compiled reagent app and return a map of pages for the javascript
  assets."
  []
  (let [js (stasis/slurp-directory "resources/public/js/compiled/" #"\.js$")]
    (rename-keys js
                 (zipmap (keys js)
                         (map #(str "/js/compiled" %) (keys js))))))

(defn basename
  [f ext]
  (clojure.string/replace (.getName f)
                          ext
                          ""))

(defn filename->id
  [coll-ns path]
  (keyword (name coll-ns)
           (basename (io/file path) ".edn")))

(defn slurp-collection!
  [coll-ns path]
  (let [sources (stasis/slurp-directory path #"\.edn$")
        names   (map #(filename->id coll-ns %) (keys sources))
        items   (map read-string (vals sources))]
    (zipmap names items)))

(defn slurp-data!
  []
  (map (fn [coll-dir]
         (let [path (.getPath coll-dir)
               name (basename coll-dir "")]
           (slurp-collection! name path)))
       (.listFiles (io/file "resources/public/data"))))

(def pages
  (merge {"/"
          (hiccup/html
            [:html
             [:script {:type "text/javascript"
                       :src  "/js/compiled/web.js"}]
             [:body
              [:h1 "Data"]
              [:pre
               (with-out-str (pprint (slurp-data!)))]]])}

         (slurp-reagent!)))

(doall
  (map println (keys pages)))

(def app (stasis/serve-pages pages))
;;