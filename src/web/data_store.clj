(ns web.data-store
  (:require [clojure.string :as string]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [stasis.core :refer [slurp-directory]]

            [web.data-store.compiler :refer [compile-edges]]
            [web.data-store.analyzer :refer [analyze]]))

(defn drop-ext
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn pull? [x] (and (sequential? x) (= 'pull (first x))))

(defn analyze-file
  [render [path source]]
  (let [ks   (path->ks path)
        form (render source)]
    (analyze pull? ks form)))

(defn analyze-dir
  "Given a map containing the following keys:

  - 'path'   : the path to the root of the data tree
  - 'ext'    : the extension of the data files
  - 'render' : a function which is called with the file contents, and returns an EDN data
               structure.

  returns a sequence of [ks edn-form] tuples."
  [{:keys [path ext render]}]
  (into []
        (mapcat #(analyze-file render %))
        (slurp-directory path (re-pattern (str "\\." ext)))))

(defn file-db
  [& configs]
  (compile-edges
    (into [] (mapcat analyze-dir) configs)))
