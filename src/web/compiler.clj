(ns web.compiler
  (:require [web.fs :refer [get-name get-path]]
            [web.readers :refer [tree-iter file-tree]]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn- read-content
  [reader]
  (->> (java.io.BufferedReader. reader)
       (line-seq)
       (string/join "\n")))

(defn- content->map
  [file render]
  (with-open [rdr (java.io.PushbackReader. (io/reader file))]
    (let [meta     (edn/read rdr)
          content  (read-content rdr)]
      {:filename (get-name file)
       :path     (get-path file)
       :meta     meta
       :content  (render content)})))

(defn file-entry-xf
  [render]
  (map (fn [[p f]] [p (render (slurp f))])))

(defn content-entry-xf
  [render]
  (map (fn content-xf [[path file]]
         [path (content->map file render)])))

(defn resource-tree
  [path {:keys [ext render] :as content-type}]
  (file-tree path
             ext
             (file-entry-xf render)))

(defn content-map
  [path {:keys [ext render] :as content-type}]
  (into {}
        (tree-iter path
                   ext
                   (content-entry-xf render))))

(defn content-tree
  [path {:keys [ext render] :as content-type}]
  (file-tree path
             ext
             (content-entry-xf render)))