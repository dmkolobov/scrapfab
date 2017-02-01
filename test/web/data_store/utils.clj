(ns web.data-store.utils
  (:require [clojure.math.combinatorics :refer [cartesian-product]]
            [ubergraph.core :as uber]
            [clojure.set :as set]))

(def mirror-xf (mapcat (juxt identity reverse)))

(defn filter-nodes
  "Returns all nodes in graph for which 'pred' is true."
  [pred graph]
  (eduction (filter pred) (uber/nodes graph)))

(defn stitch-nodes
  "Returns a new graph with 'nodes' added. The graph also includes any
   possible edges between existing nodes and the new nodes for which 'valid-edge?'
   is true."
  [graph valid-edge? nodes]
  (println "stitching" nodes)
  (uber/add-edges* (uber/add-nodes* graph nodes)
                   (into []
                         (comp mirror-xf (filter valid-edge?))
                         (cartesian-product (uber/nodes graph) nodes))))

(defn restitch-nodes
  "Returns a new graph which excludes any elements of 'old-nodes' which are not
  present in 'new-nodes'. All elements of 'new-nodes' are stitched into the graph."
  [graph valid-edge? old-nodes new-nodes]
  (-> graph
      (uber/remove-nodes* (set/difference old-nodes new-nodes))
      (stitch-nodes valid-edge? (set/difference new-nodes old-nodes))))