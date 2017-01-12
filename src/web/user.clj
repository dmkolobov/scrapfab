(ns web.user
  (:require [clojure.pprint :refer [pprint]]))

(def ^:dynamic site nil)

(defn base-layout
  [& {:keys [body]}]
  [:html
   [:body
    [:h1 "SCRAPFAB!"]
    body]])