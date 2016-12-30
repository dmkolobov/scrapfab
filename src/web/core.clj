(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]))

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
       (let [fs (.listFiles (io/file "resources/public/data"))]
         (println fs)
         fs)))

(def pages
  (merge {"/"
          (str "<html>
              <head>
                <script type=\"text/javascript\" src=\"/js/compiled/web.js\"></script>
              </head>
              <body>

              <pre>"
               (with-out-str
                 (pprint
                   (slurp-data!)))
               "</pre>
              </body>
            </html>")}

         (slurp-reagent!)))

(doall
  (map println (keys pages)))

(def app (stasis/serve-pages pages))
;;