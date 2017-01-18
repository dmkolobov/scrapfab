(ns web.core
  (:require [ring.middleware.content-type :refer [wrap-content-type]]
            [stasis.core :as stasis]

            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [prewalk postwalk-replace]]

            [hiccup.core :as hiccup]
            [markdown.core :refer [md-to-html-string]]

            [optimus.prime :as optimus]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :refer [serve-live-assets-autorefresh]]
            [optimus.export]

            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]

            [web.user]
            [web.pull :refer [analyze compile pull]]
            [web.compiler :refer [into-tree slurp-ext slurp-pages]]))

(defn md-template
  [[meta content]]
  (postwalk-replace {'(content) (md-to-html-string content)} meta))

(defn meta-content
  [source]
  (let [reader (string-push-back-reader source)
        meta   (edn/read reader)]
    (loop [c (read-char reader) s (StringBuilder.)]
      (if (some? c)
        (recur (read-char reader) (.append s c))
        [meta (str s)]))))

(def site-context
   (-> {}
       (into-tree (map (juxt first (comp edn/read-string second)))
                  (slurp-ext "resources/data" "edn"))
       (into-tree (map (juxt first (comp md-template meta-content second)))
                  (slurp-ext "resources/data" "md"))
       (compile)))

(defn get-assets
  []
  (concat
    (assets/load-bundle "public" "site.css" ["/css/fonts.css"
                                             "/css/main.css"])
    (assets/load-bundle "public" "app.js" [#"/js/compiled/out/*"
                                           "/js/compiled/web.js"])))

(defn eval-template
  [ns context source]
  (binding [*ns* (the-ns ns)]
    (with-bindings {(intern ns 'context) context}
      (eval (read-string source)))))

(defn get-pages
  []
  (slurp-pages "resources/pages"
               "clj"
               (fn [context source]
                 (let [[meta source] (meta-content source)
                       context       (assoc context
                                       :current-page (pull site-context meta))]
                   (hiccup/html (eval-template 'web.user context source))))))

(def app (-> (stasis/serve-pages get-pages)
             (optimus/wrap get-assets optimizations/all serve-live-assets-autorefresh)
             wrap-content-type))