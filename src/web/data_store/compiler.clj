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

(defn child-ks?
  [ks x]
  (= (take (count ks) x) ks))

(defn resolve-deps
  [tuples [_ & ks :as pull]]
  (into #{}
        (comp (filter (comp #(child-ks? ks %) first))
              (map (comp #(vector pull %) second)))
        tuples))

(defn compile-edges
  [ast]
  (into #{}
        (mapcat (comp (partial resolve-deps ast)
                      second))
        ast))