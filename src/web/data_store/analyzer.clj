(ns web.data-store.analyzer
  (:require [clojure.walk :refer [prewalk]]))

(defn branch?
  [pred x]
  (and (not (pred x)) (coll? x)))

(defn collect-forms
  [pred form]
  (filter pred
          (tree-seq #(branch? pred %) seq form)))

(defn analyze
  [pred ks form]
  (cond (pred form)
        [[ks form]]

        (map? form)
        (transduce (map (fn [[k v]] (analyze pred (conj (vec ks) k) v)))
                   (completing into)
                   []
                   form)

        :default
        (eduction (map #(vector ks %))
                  (collect-forms pred form))))

(defrecord PullForm [form file ks])

(defn pull-record? [x] (instance? PullForm x))