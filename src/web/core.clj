(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [hiccup.core :as hiccup]

            [web.compiler :as compiler]
            [web.content-types :as types]
            [web.scrapfab :as scrapfab]))

(def site
  {:data     (compiler/resource-tree "resources/data" types/edn)
   :art      (compiler/content-map "resources/art" types/md)
   :pages    (compiler/content-map  "resources/pages" types/md)})
;;
(def pages
  {"/" (hiccup/html
         [:div
          [:pre
          (with-out-str
            (pprint
              site))]
          [:h1 "Pages"]
          (for [[path {:keys [meta content]}] (:pages site)]
            [:div
             [:h2 path]
             [:pre (with-out-str (pprint meta))]
             content])])})

(def app (stasis/serve-pages pages))
