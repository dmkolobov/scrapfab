(ns web.readers
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as reader-types]
            [stasis.core :as stasis]))

(defn get-path [file] (.getPath file))
(defn get-name [file] (.getName file))

(defn get-relative-path
  [base file]
  (loop [b (clojure.string/split base #"/")
         p (clojure.string/split (get-path file) #"/")]
    (if (seq b)
      (recur (rest b)
             (rest p))
      (clojure.string/join "/" p))))

(def edn-pattern #".*\.edn")

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

(defn- file-filter
  [regex]
  (fn [file] (re-matches regex (get-path file))))

(defn- tree-iter
  [path regex xf]
  (eduction (comp (filter (file-filter regex))
                  xf)
            (file-seq (io/file path))))

(defn slurp-files
  [path regex]
   (into {}
         (tree-iter path
                    regex
                    (map (juxt #(get-relative-path path %) slurp)))))

(defn slurp-content
  [path regex]
  (into {}
        (tree-iter path
                   regex
                   (map (juxt #(get-relative-path path %) content->map)))))