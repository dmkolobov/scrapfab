(ns web.content-types
  (:require [hiccup.core :as hiccup]
            [markdown.core :refer [md-to-html-string]]))

(def edn
  {:ext    ".edn"
   :render read-string})

(def md
  {:ext    ".md"
   :render (comp hiccup/h md-to-html-string)})

(def html
  {:ext    ".html"
   :render identity})

(def hiccup
  {:ext   ".hiccup"
   :render (fn [source] (hiccup/html source))})