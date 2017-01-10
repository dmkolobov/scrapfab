(ns web.core
  (:require [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [clojure.walk :refer [prewalk postwalk-replace]]

            [hiccup.core :as hiccup]

            [web.pull :refer [analyze compile]]
            [web.compiler :refer [into-tree meta-file slurp-content] :as c]))

(def pull-content
  (map (fn [[path [meta content]]]
         [path (postwalk-replace {'(content) content} meta)])))

(def data-tree
  (let [edn (slurp-content "resources/data" c/edn)
        md  (slurp-content "resources/data" (meta-file c/md))]
    (-> {}
        (into-tree edn)
        (into-tree pull-content md))))

(def site
  (merge data-tree
         {:pages (into {} (slurp-content "resources/pages" (meta-file c/md)))}))

(defn pp [s] (with-out-str (pprint s)))

(def test1
  '{:debug "hello, world"
    :foo   {:text #{(pull :debug) "bar" "car"}}
    :bar   :foo
    :txt   (pull :fin)
    :fin   {:foo (pull :foo)
            :bar (pull :bar)}})

(def pages
  {"/" (hiccup/html
         [:div
          [:h1 ".SCRAPFAB."]
          [:pre (pp site)]
          [:pre (pp (compile site))]
          [:hr]
          [:pre (pp (get-in site [:art :fire_pit]))]
          [:hr]
          [:pre (pp (analyze site))]
          [:pre (pp test1)]
          [:pre (pp (analyze test1))]
          [:pre (pp (compile test1))]])})

(def app (stasis/serve-pages pages))
