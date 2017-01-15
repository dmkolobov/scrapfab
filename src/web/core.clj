(ns web.core
  (:require [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :refer [prewalk postwalk-replace]]

            [hiccup.core :as hiccup]
            [markdown.core :refer [md-to-html-string]]

            [web.user]
            [web.pull :refer [analyze compile pull]]
            [web.compiler :refer [into-tree meta-content slurp-ext] :as c]
            [clojure.tools.reader.edn :as edn]))

(defn md-template
  [[meta content]]
  (postwalk-replace {'(content) (md-to-html-string content)} meta))

(def site-context
   (-> {}
       (into-tree (map (juxt first (comp edn/read-string second)))
                  (slurp-ext "resources/data" "edn"))
       (into-tree (map (juxt first (comp md-template meta-content second)))
                  (slurp-ext "resources/data" "md"))
       (compile)))

(pprint site-context)

(defn clj->html [path] (string/replace path #"\.clj" ".html"))

(defn clj-template
  [site-ns context]
  (fn [[meta source]]
    (binding [*ns* (the-ns site-ns)]
      (with-bindings {(intern site-ns 'site) (pull context meta)}
        (eval (read-string source))))))

(defn hiccup->html [hic] (hiccup/html hic))

(def site
  (into {}
        (map (juxt (comp clj->html first)
                   (comp hiccup->html
                         (clj-template 'web.user site-context)
                         meta-content
                         second)))
        (slurp-ext "resources/pages" "clj")))

(pprint site)

(def app (stasis/serve-pages site))
