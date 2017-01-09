(ns web.readers
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as reader-types]
            [stasis.core :as stasis]

            [web.fs :refer [get-name get-path get-relative-path]]))

(defn- file-filter
  [regex]
  (fn [file] (re-matches regex (get-path file))))

(defn- ext->file-pattern
  [ext]
  (re-pattern (str ".*\\" ext)))

(defn- ext->pattern
  [ext]
  (re-pattern (str "\\" ext)))

(defn tree-iter
  [path ext xf]
  (eduction (comp (filter (file-filter (ext->file-pattern ext)))
                  (map (juxt #(get-relative-path path %) identity))
                  xf)
            (file-seq (io/file path))))

(defn path->ks
  [path re]
  (map keyword (string/split (string/replace path re "")
                             #"/")))

(defn file-ks
  [path ext]
  (path->ks path (ext->pattern ext)))

(defn file-tree
  ([path ext]
   (file-tree path ext (map identity)))

  ([path ext xf]
   (reduce (fn [tree [path x]]
             (assoc-in tree (file-ks path ext) x))
           {}
           (tree-iter path ext xf))))