(ns web.core
  (:require [ring.middleware.content-type :refer [wrap-content-type]]
            [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :refer [prewalk postwalk-replace]]

            [hiccup.core :as hiccup]
            [markdown.core :refer [md-to-html-string]]

            [optimus.prime :as optimus]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :refer [serve-live-assets-autorefresh]]
            [optimus.export]

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

(defn clj->html [path] (string/replace path #"\.clj" ".html"))

(defn get-assets
  []
  (concat
    (assets/load-bundle "public" "site.css" ["/css/fonts.css"
                                             "/css/main.css"])
    (assets/load-bundle "public" "app.js" [#"/js/compiled/out/*"
                                           "/js/compiled/web.js"])))

(defn matching-ext
  [ext]
  (fn [f]
    (re-matches (re-pattern (str ".*\\." ext))
                (.getPath f))))

(defn relative-pairs
  [path]
  (fn [f]
    [(string/replace (.getPath f) path "") f]))

(defn eval-template
  [ns context source]
  (binding [*ns* (the-ns ns)]
    (with-bindings {(intern ns 'context) context}
      (eval (read-string source)))))

(defn get-pages
  []
  (into {}
        (comp (filter (matching-ext "clj"))
              (map (relative-pairs "resources/pages"))
              (map (juxt (comp clj->html first)
                         (comp (fn [file]
                                 (fn [context]
                                   (let [[meta source] (meta-content (slurp file))
                                         context       (merge context (pull site-context meta))]
                                     (hiccup/html
                                       (eval-template 'web.user context source)))))
                               second))))
        (file-seq (io/file "resources/pages"))))

(def app (-> (stasis/serve-pages get-pages)
             (optimus/wrap get-assets optimizations/all serve-live-assets-autorefresh)
             wrap-content-type))