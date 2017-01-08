(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [hiccup.core :as hiccup]

            [web.scrapfab :as scrapfab]))

(defn slurp-css!
  []
  (let [css (stasis/slurp-directory "resources/public/css" #"\.css$")]
    (rename-keys css
                 (zipmap (keys css)
                         (map #(str "/css" %) (keys css))))))

(defn slurp-js!
  []
  (let [js (stasis/slurp-directory "resources/public/js/compiled/" #"\.js$")]
    (rename-keys js
                 (zipmap (keys js)
                         (map #(str "/js/compiled" %) (keys js))))))

(defn with-doctype
  "Prepends the HTML5 doctype tag to the given HTML."
  [html]
  (str "<!DOCTYPE html>" html))

(defn mk-render-fn
  [url theme context]
  (let [context (assoc context
                  :url url
                  :js  (:js theme)
                  :css (:css theme)
                  :art (slurp-content "resources/public/data/art" edn-pattern))]
    (fn render-fn
      ([id]
       (render-fn id context))
      ([id new-context]
       (let [layout-fn (get-in theme [:layouts id])]
         (layout-fn render-fn new-context))))))

(defn render-page
  [theme [url {:keys [layout] :as context}]]
  (let [render (mk-render-fn url theme context)]

    [url (render layout)]))

(defn wrap-page
  [[url hiccup]]
  [url (with-doctype (hiccup/html hiccup))])

  (defn render-site
    [theme site]
    (merge
      (into {}
            (map (comp wrap-page
                       (partial render-page theme))
                 site))
      (slurp-css!)
      (slurp-js!)))

(def pages
  (render-site scrapfab/theme scrapfab/site))

(def app (stasis/serve-pages pages))
;;