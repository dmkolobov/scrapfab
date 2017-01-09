(ns web.compiler
  (:require [web.fs :refer [drop-ext]]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :refer [postwalk-replace]]
            [stasis.core :refer [slurp-directory]]
            [markdown.core :refer [md-to-html-string]]
            [hiccup.core :as hiccup]))

;; ---- default content types ----

(def edn
  {:ext "edn"
   :render  read-string})

(def md
  {:ext "md"
   :render  (comp hiccup/h md-to-html-string)})

(def html
  {:ext "html"
   :render  identity})

(def hiccup
  {:ext "hiccup"
   :render  (fn [source] (hiccup/html source))})

(defn meta-content-pair
  [source render]
  (let [reader (string-push-back-reader source)
        meta   (edn/read reader)]
    (loop [c (read-char reader)
           s (StringBuilder.)]
      (if (some? c)
        (recur (read-char reader)
               (.append s c))
        [meta (render (str s))]))))

;; ---- public api ----

(defn content-pattern
  [content-type]
  (re-pattern
    (str "\\." (:ext content-type))))

(defn content-xf
  [content-type]
  (let [render (:render content-type)]
    (map (fn [[path source]] [path (render source)]))))

(defn meta-file
  [{:keys [ext render]}]
  {:ext    ext
   :render (fn [source] (meta-content-pair source render))})

(defn slurp-content
  [path content-type]
  (eduction (content-xf content-type)
            (slurp-directory path (content-pattern content-type))))

(defn path->ks
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn add-tree-entry
  [tree [path x]]
  (assoc-in tree (path->ks path) x))

(defn into-tree
  ([tree content-map]
   (reduce add-tree-entry tree content-map))
  ([tree xf content-map]
   (transduce xf (completing add-tree-entry) tree content-map)))

(defn as-tree
  ([content-map] (into-tree {} content-map))
  ([xf content-map] (into-tree {} xf content-map)))
