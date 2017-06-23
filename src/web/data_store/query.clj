(ns web.data-store.query
  (:require [ubergraph.alg :as alg]
            [ubergraph.core :as uber]

            [web.data-store.compiler :refer [analyze-form]]
            [web.data-store.forms :refer [node-ks emit valid-edge?]]
            [web.data-store.utils :refer [stitch-nodes transitive-deps]]))

(defn evaluate
  [db contexts order]
  (reduce (fn [contexts node] (emit node db contexts))
          contexts
          order))

(defn root-context
  [root-forms]
  (reduce (partial apply assoc-in) {} root-forms))

(defn in-child-context? [{:keys [context]}] (not= [] context))

(defn branches-only
  [nodes]
  (let [nodemap (group-by node-ks nodes)]
    (comp (filter in-child-context?)
          (map (fn [node]
                 (first (get nodemap (:context node)))))
          (distinct))))

(def query-state-reducer
  (completing
    (fn [state node]
      (assoc state (node-ks node) (vec (:form node))))))

(defn query-state
  [root-forms nodes]
  (transduce (branches-only nodes)
             query-state-reducer
             {[] (root-context root-forms)}
             nodes))

(defn eval-order
  [graph nodes]
  (keep (transitive-deps graph nodes) (alg/topsort graph)))

(defn query
  [db q-form]
  (let [{:keys [graph forms] :as state} @db
        q-nodes  (into #{} (analyze-form [:__query] q-form))
        q-graph  (stitch-nodes graph valid-edge? q-nodes)
        q-forms  (assoc forms [:__query] q-form)
        order    (eval-order q-graph q-nodes)]
    (get-in (evaluate (assoc state :graph q-graph :forms q-forms)
                      (query-state q-forms order)
                      order)
            [[] :__query])))