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

            [web.user]
            [web.string-readers :refer [with-edn-header]]
            [web.data-store :refer [file-db pull]]
            [web.pages :refer [slurp-pages]]
            [clojure.tools.reader.edn :as edn]))

(defn md-template
  [[meta content]]
  (postwalk-replace {'(content) (md-to-html-string content)} meta))

(def data-store
  (file-db {:path   "resources/data"
            :ext    "edn"
            :render edn/read-string}

           {:path   "resources/data"
            :ext    "md"
            :render (comp md-template with-edn-header)}))

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
                 (let [[meta source] (with-edn-header source)
                       context       (assoc context
                                       :current-page (pull data-store meta))]
                   (hiccup/html (eval-template 'web.user context source))))))

(def app (-> (stasis/serve-pages get-pages)
             (optimus/wrap get-assets optimizations/all serve-live-assets-autorefresh)
             wrap-content-type))