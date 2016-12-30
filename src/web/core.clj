(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]))

(defn slurp-reagent!
  []
  (let [js (stasis/slurp-directory "resources/public/js/compiled/" #"\.js$")]
    (rename-keys js
                 (zipmap (keys js)
                         (map #(str "/js/compiled" %) (keys js))))))

(def pages
  (merge {"/"
            "<html>
              <head>
                <script type=\"text/javascript\" src=\"/js/compiled/web.js\"></script>
              </head>
              <body>
                Hello, world!
              </body>
            </html>"}

         (slurp-reagent!)))

(doall
  (map println (keys pages)))

(def app (stasis/serve-pages pages))
;;