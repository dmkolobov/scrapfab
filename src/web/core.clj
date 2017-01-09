(ns web.core
  (:require [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [clojure.walk :refer [prewalk]]

            [hiccup.core :as hiccup]

            [web.compiler :as compiler]
            [web.content-types :as types]
            [web.scrapfab :as scrapfab]))

(def site
  (merge  (compiler/resource-tree "resources/data" types/edn)
  {:art   (compiler/content-tree "resources/art" types/md)
   :pages (compiler/content-map  "resources/pages" types/md)}))

(defn pull-data
  "Recursively replace lists of the form (pull & ks) with the value of (get-in db ks)."
  [db data]
  (prewalk (fn [form]
             (if (and (list? form) (= 'pull (first form)))
               (let [data' (get-in db (rest form))]
                 (pull-data db data'))
               form))
           data))

(defn pp [s] (with-out-str (pprint s)))

(def pages
  {"/" (hiccup/html
         [:div
          [:h1 "SITE666"]
          [:pre (pp site)]
          [:hr]
          [:pre (pp (get-in site [:art :fire_pit]))]
          [:hr]
          [:pre (pp
                  (pull-data site
                             site))]])})

(def app (stasis/serve-pages pages))
