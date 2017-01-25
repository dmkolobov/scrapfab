(ns web.data-store
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [stasis.core :refer [slurp-directory]]
            [clojure.tools.reader.edn :as edn]))

(defn drop-ext
  "Given a path, retur"
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  "Converts a file path to a keyword sequence as in the second argument
   to 'assoc-in'."
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn pull? [x] (and (sequential? x) (= 'pull (first x))))

;; ---------- READING ------------

(defn form-seq
  [{:keys [path ext render]}]
  (eduction (map (juxt first (comp render second)))
            (slurp-directory path (re-pattern (str "\\." ext)))))

(defn read-forms
  [configs]
  (transduce (map form-seq) (completing into) [] configs))

;; ---------- ANALISYS ------------

(defn branch?
  [pred x]
  (and (not (pred x)) (coll? x)))

(defn collect-forms
  [pred form]
  (filter pred
          (tree-seq #(branch? pred %) seq form)))

(defn analyze
  "Returns a sequence of [ks sub-form] tuples, where 'pred' returns true
  for 'sub-form', and 'ks' is the path to 'sub-form' in the original
  'form', as defined in 'get-in'. If the argument 'ks' is given,
  it is used as a prefix to each path returned."
  ([pred form]
   (analyze pred [] form))

  ([pred ks form]
   (cond (pred form)
         [[ks form]]

         (map? form)
         (transduce (map (fn [[k v]] (analyze pred (conj (vec ks) k) v)))
                    (completing into)
                    []
                    form)

         :default
         (eduction (map #(vector ks %))
                   (collect-forms pred form)))))

(defrecord PullForm [form file ks])

(defn pull-record? [x] (instance? PullForm x))

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

;; ---------- COMPILATION ------------

(defn find-deps
  [index {:keys [form] :as vert}]
  (into #{}
        (map (fn [child-vert] [vert child-vert]))
        (collect-forms pull-record? (get-in index (rest form)))))

(defn index-ast
  [ast]
  (reduce (fn [index {:keys [ks] :as vert}] (assoc-in index ks vert))
          {}
          ast))

(defn compile-graph
  [ast]
  (transduce (map (partial find-deps (index-ast ast)))
             (completing #(apply uber/add-edges %1 %2))
             (uber/digraph)
             ast))

;; ---------- PUBLIC API -------------

(defn add-context
  [context [path form]]
  (assoc-in context (path->ks path) form))

(defn file-db
  [& configs]
  (let [forms (read-forms configs)
        graph (->> forms (analyze-forms) (compile-graph))]
    {:forms   forms
     :graph   graph
     :context (reduce add-context {} forms)}))

(defn resolve-db
  [{:keys [graph context] :as db}]
  (reduce (fn [smap {:keys [form]}]
            (assoc smap
              form (walk/postwalk-replace smap (get-in context (rest form)))))
          {}
          (reverse (alg/topsort graph))))

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