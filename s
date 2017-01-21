(ns web.data-store
  (:require [stasis.core :refer [slurp-directory]]
            [clojure.walk :refer [prewalk]]
            [clojure.string :as string]))

(defn pull-form? [form] (and (list? form) (= 'pull (first form))))

(defn walk-pulls
  [state ks form]
  (when (pull-form? form)
    (swap! state conj [ks form]))
  form)

(defn walk-ks
  [state ks form]
  (if (map? form)
    (doseq [[k v] form]
      (walk-ks state (conj ks k) v))
    (prewalk #(walk-pulls state ks %) form)))

(defn analyze-form
  "Return a set of [ks pull] tuples, where 'ks' is the location
  of 'pull'."
  ([form]
   (analyze-form [] form))
  ([prefix-ks form]
   (let [state (atom #{})]
     (walk-ks state prefix-ks form)
     @state)))

(defn path->ks
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn read-forms
  "Given a map containing the following keys:

  - 'path'   : the path to the root of the data tree
  - 'ext'    : the extension of the data files
  - 'render' : a function which is called with the file contents, and returns an EDN data structure.

  returns a sequence of [ks edn-form] tuples."
  [{:keys [path ext render]}]
  (eduction (map (juxt (comp path->ks first)
                       (comp render second)))
            (slurp-directory path (re-pattern (str "\\." ext)))))

(defn pull-matcher
  [pull]
  (let [args (rest pull)
        ct   (count args)]
    (fn [[ks _]] (= args (take ct ks)))))

(defn resolve-deps
  [tuples pull]
  (into #{}
        (comp (filter (pull-matcher pull))
              (map (comp #(vector % pull) second)))
        tuples))

(defn tuples->edges
  "Given a sequence of [ks pull] tuples, return a sequence of [pull pull-dep] edges.
  If a pull has no dependencies, [pull nil] is returned for that pull."
  [tuples]
  (into #{}
        (map (comp (partial resolve-deps tuples) second))
        tuples))

'{:foo "foo"
  :bar "bar"
  :one (pull :foo)
  :two (pull :bar)
  :debug {:hello (pull :one)
          :world (pull :two)}
  :final (pull debug)}

(assert
  (= (tuples->edges
      '[[[:one]          (pull :foo)]
        [[:two]          (pull :bar)]
        [[:debug :hello] (pull :one)]
        [[:debug :world] (pull :two)]
        [[:final]        (pull :debug)]])

     '#{[(pull :one)   (pull :foo)]
        [(pull :two)   (pull :bar)]
        [(pull :debug) (pull :one)]
        [(pull :debug) (pull :two)]}))

(def tuple-xf
  (comp (mapcat read-forms)
        (mapcat (partial apply analyze-form))))

(defn analyze
  [& configs]
  (tuples->edges (into [] tuple-xf configs)))

(defn free-vertices
  [edges]
  (into #{}
        (comp (map reverse)
              (filter (comp empty?)))
        edges))