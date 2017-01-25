(ns web.data-store
  (:require [clojure.string :as string]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [stasis.core :refer [slurp-directory]]

            [web.data-store.compiler :refer [compile-edges]]
            [web.data-store.analyzer :refer [analyze map->PullForm]]
            [clojure.tools.reader.edn :as edn]))

(defn drop-ext
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn pull? [x] (and (sequential? x) (= 'pull (first x))))


(defn form-seq
  [{:keys [path ext render]}]
  (eduction (map (juxt first (comp render second)))
            (slurp-directory path (re-pattern (str "\\." ext)))))

(defn read-roots
  [configs]
  (transduce (map form-seq) (completing into) [] configs))

(def analyze-xf
  (comp (map (juxt (comp path->ks first) second))
        (map (juxt first (partial apply analyze pull?)))
        (map (fn [[path entries]]
               (eduction (map (fn [[ks sub-form]]
                                (map->PullForm
                                  {:form sub-form :file path :ks ks})))
                         entries)))))

(defn analyze-forms
  [form-seq]
  (transduce analyze-xf (completing into) [] form-seq))

(defn file-db
  [& configs]
  (let [form-seq (read-roots configs)]
    {:forms form-seq
     :graph (->> form-seq
                 (analyze-forms)
                 (compile-edges)
                 (apply uber/digraph))}))

(defn fx
  []
  (file-db {:path   "resources/data"
            :ext    "edn"
            :render edn/read-string}

           {:path   "resources/data"
            :ext    "md"
            :render edn/read-string}

           {:path   "resources/pages"
            :ext    "clj"
            :render edn/read-string}))