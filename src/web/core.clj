(ns web.core
  (:require [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [clojure.walk :refer [prewalk postwalk-replace]]

            [hiccup.core :as hiccup]

            [web.pull :refer [pull]]
            [web.compiler :as c]))

(def pull-content
  (map (fn [[path [meta content]]]
         [path (postwalk-replace {'(content) content} meta)])))

(def data-tree
  (let [edn (c/slurp-content "resources/data" c/edn)
        md  (c/slurp-content "resources/data" (c/meta-file c/md))]
    (-> {}
        (c/into-tree edn)
        (c/into-tree pull-content md))))

(def site
  (merge data-tree
         {:pages (into {} (c/slurp-content "resources/pages" (c/meta-file c/md)))}))

(defn pp [s] (with-out-str (pprint s)))

(def pages
  {"/" (hiccup/html
         [:div
          [:h1 "SCRAPFAB!"]
          [:pre (pp site)]
          [:hr]
          [:pre (pp (get-in site [:art :fire_pit]))]
          [:hr]
          [:pre (pp (pull site site))]])})

(def app (stasis/serve-pages pages))
