(ns web.render
  (:require [hiccup.core :as hiccup]))

(def layouts (atom {}))

(defn register-layout
  [id f]
  (swap! layouts assoc id f))

(defn- with-doctype
  "Prepends HTML5 doctype to the given html."
  [html]
  (str "<!DOCTYPE html>" html))

(defn render-page
  [data]
  (let [layout-fn (get @layouts (:layout data))]
    (with-doctype
      (hiccup/html
        (apply layout-fn (mapcat identity data))))))