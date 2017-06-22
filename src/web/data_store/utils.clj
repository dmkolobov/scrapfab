(ns web.data-store.utils
  (:require [clojure.math.combinatorics :refer [cartesian-product]]
            [ubergraph.core :as uber]
            [clojure.set :as set]))

(def mirror-xf (mapcat (juxt identity reverse)))

;; ------------------- graph functions -------------------

(defn stitch-nodes
  "Returns a new graph with 'nodes' added. The graph also includes any
   possible edges between existing nodes and the new nodes for which 'valid-edge?'
   is true."
  [graph valid-edge? nodes]
  (uber/add-edges* (uber/add-nodes* graph nodes)
                   (into []
                         (comp mirror-xf (filter valid-edge?))
                         (into (cartesian-product (uber/nodes graph) nodes)
                               (cartesian-product nodes nodes)))))

(defn restitch-nodes
  "Returns a new graph which excludes any elements of 'old-nodes' which are not
  present in 'new-nodes'. All elements of 'new-nodes' are stitched into the graph."
  [graph valid-edge? old-nodes new-nodes]
  (-> graph
      (uber/remove-nodes* (set/difference old-nodes new-nodes))
      (stitch-nodes valid-edge? (set/difference new-nodes old-nodes))))

;; ------------------- collection functions -------------------

(defn collect-forms
  "Returns a sequence of [ks sub-form] tuples where the value (get-in form ks)
  contains the sub-form, and pred is true for sub-form."
  [pred form]
  (filter pred
          (tree-seq #(and (not (pred %)) (coll? %))
                    seq
                    form)))

;; -----------