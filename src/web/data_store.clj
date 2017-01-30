(ns web.data-store
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [stasis.core :refer [slurp-directory]]))

(defn drop-ext
  "Given a path, retur"
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  "Converts a file path to a keyword sequence as in the second argument
   to 'assoc-in', 'get-in', etc."
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn require?
  [x]
  (and (sequential? x) (= 'require (first x))))

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
  (filter pred (tree-seq #(branch? pred %) seq form)))

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

(defrecord RequireForm [form file ks])

(defn require-record? [x] (instance? RequireForm x))

(def analyze-xf
  (comp (map (juxt first (comp (partial apply analyze require?)
                               (juxt (comp path->ks first) second))))
        (map (fn [[path entries]]
               (eduction (map (fn [[ks require-form]]
                                (map->RequireForm {:form require-form :file path :ks ks})))
                         entries)))))

(defn analyze-forms
  [form-seq]
  (transduce analyze-xf (completing into) [] form-seq))

;; ---------- COMPILATION ------------

(def add-edges-reducer (completing uber/add-edges))

(defn add-deps
  [index graph {:keys [form] :as v}]
  (let [children (collect-forms require-record? (get-in index (rest form)))]
    (if (seq children)
      (transduce (map (fn [c] [v c])) add-edges-reducer graph children)
      (uber/add-nodes graph v))))

(defn index-ast
  [ast]
  (reduce (fn [index {:keys [ks] :as vert}] (assoc-in index ks vert))
          {}
          ast))

(defn compile-graph
  [graph ast]
  (let [index (index-ast ast)]
    (reduce (partial add-deps index)
            graph
            ast)))

;; ---------- PUBLIC API -------------

(defn add-context
  [context [path form]]
  (assoc-in context (path->ks path) form))

(defn build
  [graph context]
  (reduce (fn [smap {:keys [form]}]
            (assoc smap
              form (walk/postwalk-replace smap (get-in context (rest form)))))
          {}
          (reverse (alg/topsort graph))))

(defn rebuild-ast
  [{:keys [forms] :as new-state}]
  (println "rebuilding ast.")
  (assoc new-state
    :ast     (analyze-forms forms)
    :context (reduce add-context {} forms)))

(defn rebuild-graph
  [old-state {:keys [ast] :as new-state}]
  (if (= ast (:ast old-state))
    new-state
    (do
      (println "rebuilding graph.")
      (assoc new-state :graph (compile-graph (uber/digraph) ast)))))

(defn rebuild-smap
  [{:keys [context graph] :as state}]
  (assoc state :smap (build graph context)))

(defn db-watcher
  [key ref old-state new-state]
  (when (not= (:forms new-state) (:forms old-state))
    (println "rebuilding...")
    (reset! ref
            (->> new-state
                 (rebuild-ast)
                 (rebuild-graph old-state)
                 (rebuild-smap)))))

(defn watch-db
  [db]
  (add-watch db :file-watcher db-watcher)
  db)

(defn file-db
  [& configs]
  (let [forms (read-forms configs)
        db    (watch-db (atom {}))]
    (reset! db {:forms forms})
    db))

(defn pull
  [db q-form]
  (let [{:keys [context smap]} @db]
    (walk/postwalk (fn [sub-form]
                     (if (require? sub-form)
                       (walk/postwalk-replace smap
                                              (get-in context (rest sub-form)))
                       sub-form))
                   q-form)))