(ns web.data-store.compiler
  (:require [web.data-store.analyzer :as ana]
            [clojure.walk :refer [prewalk-replace]]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [clojure.pprint :refer [pprint]]))

(defn find-deps
  [index {:keys [form] :as vert}]
  (into #{}
        (map (fn [child-vert] (vector vert child-vert {})))
        (ana/collect-forms ana/pull-record? (get-in index (rest form)))))

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