(ns web.core
  (:require [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :refer [prewalk postwalk-replace]]

            [hiccup.core :as hiccup]

            [web.user]
            [web.pull :refer [analyze compile pull]]
            [web.compiler :refer [into-tree meta-file slurp-content] :as c]))

(def pull-content
  (map (fn [[path [meta content]]]
         [path (postwalk-replace {'(content) content} meta)])))

(def site-context
  (let [edn (slurp-content "resources/data" c/edn)
        md  (slurp-content "resources/data" (meta-file c/md))]
    (-> {}
        (into-tree edn)
        (into-tree pull-content md)
        (compile))))

(defn clj->html [path] (string/replace path #"\.clj" ".html"))

(defn eval-clj
  [site-ns context [meta source]]
  (binding [*ns* (the-ns site-ns)]
    (with-bindings {(intern site-ns 'site) (pull context meta)}
      (eval source))))

(def site
  (into {}
        (map (fn [[path meta-clj]]
               [(clj->html path)
                (hiccup/html
                  (eval-clj 'web.user site-context meta-clj))]))
        (slurp-content "resources/pages" (meta-file c/hiccup))))

(def app (stasis/serve-pages site))
