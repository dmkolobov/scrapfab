(ns web.compiler
  (:require [web.fs :refer [get-name get-path]]
            [web.readers :refer [tree-iter]]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn- read-content
  [reader]
  (->> (java.io.BufferedReader. reader)
       (line-seq)
       (string/join "\n")))

(defn- content->map
  [file]
  (with-open [rdr (java.io.PushbackReader. (io/reader file))]
    (let [meta     (edn/read rdr)
          content  (read-content rdr)]
      {:filename (get-name file)
       :path     (get-path file)
       :meta     meta
       :content  content})))

(defn slurp-content
  [path ext]
  (into {}
        (tree-iter path
                   ext
                   (map (fn [[path file]] [path (content->map file)])))))
