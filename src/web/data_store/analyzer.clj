(ns web.data-store.analyzer
  (:require [clojure.walk :refer [prewalk]]))

(defn analyze-walk
  [pred state ks form]
  (when (pred form)
    (swap! state conj [ks form]))
  form)

(defn analyze-form
  [pred state ks form]
  (if (map? form)
    (doseq [[k v] form] (analyze-form pred state (conj ks k) v))
    (prewalk #(analyze-walk pred state ks %) form)))

(defn analyze
  "Given an EDN 'form' and a 'pred' function, recursively walk 'form' while
  keeping track of the path (as in the get-in, update-in, assoc-in, etc)
  and return a set of [path sub-form] tuples, where 'sub-form' is a child
  for which 'pred' returns true."
  [pred ks form]
  (let [state (atom #{})]
    (analyze-form pred state (vec ks) form)
    @state))