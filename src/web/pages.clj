(ns web.pages
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :refer [postwalk-replace]]
            [stasis.core :refer [slurp-directory]]
            [markdown.core :refer [md-to-html-string]]
            [hiccup.core :as hiccup]))

(defn matching-ext
  [ext]
  (fn [f]
    (re-matches (re-pattern (str ".*\\." ext))
                (.getPath f))))

(defn relative-pairs
  [path]
  (fn [f]
    [(string/replace (.getPath f) path "") f]))

(defn ext->html
  "Returns a function which will convert any paths with 'ext' as the extension
  to URLs ending with .html"
  [ext]
  (fn [path]
    (string/replace path (re-pattern (str "\\." ext)) ".html")))

(defn render-page
  [f]
  (fn [file]
    (fn [context] (f context (slurp file)))))

(defn slurp-pages
  [path ext f]
  (into {}
        (comp (filter (matching-ext ext))
              (map (comp (juxt (comp (ext->html ext) first)
                               (comp (render-page f) second))
                         (relative-pairs path))))
        (file-seq (io/file path))))