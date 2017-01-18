(ns web.user
  (:require [clojure.pprint :refer [pprint]]
            [optimus.html]))

(def ^:dynamic context nil)

(defn base-layout
  [& {:keys [body]}]
  [:html
   [:head
    (optimus.html/link-to-css-bundles context ["site.css"])
    [:script {:type "text/javascript"
              :src "/js/compiled/web.js"}]]
   [:body
    [:div
     [:div.scrap-logo "SCRAP"]
     [:div.fab-logo "FAB"]]
    body
    [:pre
     (with-out-str
       (pprint
         (:current-page context)))]]])