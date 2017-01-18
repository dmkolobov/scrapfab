(ns web.graph
  (:require [clojure.set :refer [difference]]))

(defn assoc-set
  [m [v dep]]
  (update m v (fn [x] (if x (conj x dep) (if dep #{dep} #{})))))

(def free-xf
  (comp (filter (comp empty? second)) (map first)))

(defn remove-free
  [deps free-set]
  (reduce (fn [deps [v v-deps]]
            (assoc deps v (difference v-deps free-set)))
          {}
          (apply dissoc deps free-set)))

(defn topsort
  [edges]
  (loop [order []
         deps  (reduce assoc-set {} edges)]
    (if (seq deps)
      (let [free-set (into #{} free-xf deps)]
        (if (seq free-set)
          (recur (into order free-set) (remove-free deps free-set))
          (throw (Error. "No topological sort found. A cycle is present."))))
      order)))