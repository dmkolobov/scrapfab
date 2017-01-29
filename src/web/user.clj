(ns web.user
  (:require [clojure.pprint]
            [optimus.html]))

(def ^:dynamic context nil)

(defn pp
  [x]
  (with-out-str (clojure.pprint/pprint x)))

(defn logo
  []
  [:div.pure-g
   [:div.pure-u-1-3.pure-u-md-1-1.scrap-logo "scrap"]
   [:div.pure-u-2-3.pure-u-md-1-1.fab-logo "fab"]])

(defn base-layout
  [& {:keys [body]}]
  [:html
   [:head
    (optimus.html/link-to-css-bundles context ["site.css"])

    [:link {:rel "stylesheet" :href "https://unpkg.com/purecss@0.6.2/build/pure-min.css"}]
    [:link {:rel "stylesheet" :href "https://unpkg.com/purecss@0.6.2/build/grids-responsive-min.css"}]

    [:script {:type "text/javascript"
              :src "/js/compiled/web.js"}]]
   [:body
    [:div.pure-g
     [:div.pure-u-1-1.pure-u-md-1-3 (logo)]
     [:div.pure-u-1-1.pure-u-md-2-3 ""]]
    body]])