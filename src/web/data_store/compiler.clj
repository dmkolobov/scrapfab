(ns web.data-store.compiler
  (:require [web.data-store.analyzer :as ana]
            [clojure.walk :refer [prewalk-replace]]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]))

(def example-form
  '{:foo "foo."
    :bar "bar."
    :one ["one" (pull :foo)]
    :two ["two" (pull :bar)]
    :debug {:hello (pull :one)
            :world (pull :two)}
    :x [(pull :debug :hello) 333 (pull :debug :world)]
    :y #{666 (pull :debug) (pull :x)}})

(defn pull? [x] (and (sequential? x) (= 'pull (first x))))

(defn find-deps
  [index {:keys [arg-ks form]}]
  (into #{}
        (map (fn [[_ child-form]] (vector form child-form)))
        (ana/analyze pull? [] (get-in index arg-ks))))

(defn index-ast
  [ast]
  (reduce (fn [index {:keys [ks form]}]
            (assoc-in index ks form))
          {}
          ast))

(defn compile-edges
  [ast]
  (transduce (map (partial find-deps (index-ast ast)))
             (completing into)
             #{}
             ast))