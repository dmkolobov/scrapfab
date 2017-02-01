(ns web.data-store.dep-graph
  (:require [clojure.math.combinatorics :refer [cartesian-product]]
            [clojure.set :as set]
            [ubergraph.core :as uber]))

(defn direct-ancestor?
  "Returns true if 'x' is a direct ancestor of 'node'."
  [node x]
  (let [ks (rest (:form x))
        ct (count ks)]
    (= ks (take ct (:ks node)))))

(defn possible-edges
  "Returns a sequence of all possible edges between every node in graph g and
  every node in 'new-nodes'."
  [g new-nodes]
  (cartesian-product (uber/nodes g) new-nodes))

(def valid-edges-xf
  (comp (mapcat (juxt identity reverse))
        (filter (partial apply direct-ancestor?))))

(defn- add-nodes
  [graph nodes]
  (let [edges (into [] valid-edges-xf (possible-edges graph nodes))]
    (-> graph
        (uber/add-nodes* nodes)
        (uber/add-edges* edges))))

(defn- requires->nodes
  [path reqs]
  (into #{} (map (fn [[ks form]] {:path path :ks ks :form form})) reqs))

(defn file-nodes
  "Returns the set of nodes in graph 'g' which are defined in the file
  residing at the file path 'p'."
  [g p]
  (into #{} (filter #(= p (:path %))) (uber/nodes g)))

(defn add-file
  "Returns a new graph where each [ks require-form] tuple in 'requires' is converted
  to a node and added to 'graph'. For each added node, the edge [new-node existing-node]
  is added if 'existing-node' depends on 'new-node', and the edge [existing-node new-node]
  is added if the reverse is true."
  [graph path requires]
  (add-nodes graph (requires->nodes path requires)))

(defn mod-file
  "Given the graph 'g', the file path 'p', and the set of require forms currently
  defined in the file at 'p', return the graph 'g' with the following modifications:

  - nodes in the graph 'g' which are defined in the file at 'p' are removed if they
    are not present in the nodes resulting from 'mod-requires'.

  - nodes which result from 'mod-requires', but are not already in the graph 'g'
    are added to the graph as with 'add-file'."
  [g p mod-requires]
  (let [prev-nodes (file-nodes g p)
        mod-nodes  (requires->nodes p mod-requires)]
    (-> g
        (uber/remove-nodes* (set/difference prev-nodes mod-nodes))
        (add-nodes (set/difference mod-nodes prev-nodes)))))

(defn rm-file
  "Returns the graph 'g' without the nodes defined in the file at 'path'.
  All edges from and to these nodes are removed as well."
  [g p]
  (uber/remove-nodes* g (file-nodes g p)))
