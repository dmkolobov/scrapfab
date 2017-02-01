(ns web.data-store.dep-graph-test
  (:require [clojure.test :refer :all]
            [ubergraph.core :as uber]
            [web.data-store.dep-graph :refer [file-nodes add-file mod-file rm-file]]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]))

'{:foo :hello-world
  :bar {:debug (require :foo)}
  :car (require :bar)
  :tst (require :car)}


(def one
  '["bar.txt" #{[[:bar :debug] (require :foo)]}])

(def two
  '["car.txt" #{[[:car] (require :bar)]}])

(def three
  '["tst.txt" #{[[:tst] (require :car)]}])

(defn node-edge-sets
  [g]
  [(into #{} (map :form) (uber/nodes g))
   (into #{}
         (map (juxt (comp :form :src)
                    (comp :form :dest)))
         (uber/edges g))])

(deftest dep-graph-test
  (let [g1 (apply add-file (uber/digraph) one)
        g2 (apply add-file g1 three)
        g3 (apply add-file g2 two)
        g4 (rm-file g3 "car.txt")
        g5 (rm-file g4 "tst.txt")
        g6 (mod-file g3 "car.txt" '#{[[:car] (require :bar :debug)]})]

    (testing "the graph should have one node and no edges."
      (let [[nodes edges] (node-edge-sets g1)]
        (is (= nodes '#{(require :foo)}))

        (is (= edges #{}))))

    (testing "the graph should have two nodes and no edges."
      (let [[nodes edges] (node-edge-sets g2)]
        (is (= nodes '#{(require :foo)
                        (require :car)}))

        (is (= edges #{}))))

    (testing "the graph should have three nodes and two edges."
      (let [[nodes edges] (node-edge-sets g3)]
        (is (= nodes '#{(require :foo)
                        (require :bar)
                        (require :car)}))

        (is (= edges '#{[(require :foo) (require :bar)]
                        [(require :bar) (require :car)]}))

        (is (= (into []
                     (map :form)
                     (alg/topsort g3))
               '[(require :foo)
                 (require :bar)
                 (require :car)]))))

    (testing "mod-file replace the node."
      (let [[nodes edges] (node-edge-sets g6)]
        (is (= nodes '#{(require :foo)
                        (require :bar :debug)
                        (require :car)}))

        (is (= edges '#{[(require :foo)        (require :bar :debug)]
                        [(require :bar :debug) (require :car)]}))

        (is (= (into []
                     (map :form)
                     (alg/topsort g6))
               '[(require :foo)
                 (require :bar :debug)
                 (require :car)]))))


    (testing
      "Removing the files in the order they were added should result in the
      same graphs in reverse order."

      (is (= g4 g2))
      (is (= g5 g1)))))