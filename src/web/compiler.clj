(ns web.compiler
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :refer [postwalk-replace]]
            [stasis.core :refer [slurp-directory]]
            [markdown.core :refer [md-to-html-string]]
            [hiccup.core :as hiccup]))

(defn drop-ext
  [path]
  (first (string/split path #"\.")))

(defn meta-content
  [source]
  (let [reader (string-push-back-reader source)
        meta   (edn/read reader)]
    (loop [c (read-char reader) s (StringBuilder.)]
      (if (some? c)
        (recur (read-char reader) (.append s c))
        [meta (str s)]))))

(defn slurp-ext
  [path ext]
  (slurp-directory path (re-pattern (str "\\." ext))))

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
