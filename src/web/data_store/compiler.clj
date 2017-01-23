(ns web.data-store.compiler
  (:require [web.data-store.analyzer :as ana]
            [clojure.walk :refer [prewalk-replace]]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [clojure.pprint :refer [pprint]]))

(def example-form
  '{:foo "foo."
    :bar "bar."
    :one ["one" (pull :foo)]
    :two ["two" (pull :bar)]
    :debug {:hello (pull :one)
            :world (pull :two)}
    :x [(pull :debug :hello) 333 (pull :debug :world)]
    :y #{666 (pull :debug) (pull :x)}})

(defn find-deps
  [index {:keys [ks arg-ks] :as vert}]
  (into #{}
        (map (fn [[_ child-vert]]
               (vector vert child-vert {})))
        (ana/analyze ana/pull-record?
                     [arg-ks]
                     (let [tree (get-in index arg-ks)]
                       #{tree}))))

(defn index-ast
  [ast]
  (reduce (fn [index {:keys [ks] :as vert}]
            (assoc-in index ks vert))
          {}
          ast))

(defn compile-edges
  [ast]
  (transduce (map (partial find-deps (index-ast ast)))
             (completing into)
             #{}
             ast))