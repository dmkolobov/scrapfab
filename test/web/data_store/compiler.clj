(ns web.data-store.compiler
  (:require [clojure.core.async :as async]
            [web.data-store.utils :refer [filter-nodes stitch-nodes restitch-nodes]]
            [clojure.tools.reader.edn :as edn]
            [ubergraph.core :as uber]
            [clojure.string :as string]))

(defn drop-ext
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

;; ------------------- analysis ----------------------

(defn branch?
  [pred x]
  (and (not (pred x)) (coll? x)))

(defn collect-forms
  [pred form]
  (filter pred (tree-seq #(branch? pred %) seq form)))

(defn require?
  [x]
  (and (sequential? x) (= 'require (first x))))

(defn analyze-form
  ([pred form]
   (analyze-form pred [] form))

  ([pred ks form]
   (cond (pred form)
         [[ks form]]

         (map? form)
         (transduce (map (fn [[k v]]
                           (analyze-form pred (conj (vec ks) k) v)))
                    (completing into)
                    []
                    form)

         :default
         (eduction (map #(vector ks %)) (collect-forms pred form)))))

(defn analyze-file
  [path]
  (let [form (edn/read-string (slurp path))]
    {:path  path
     :form  form
     :nodes (into #{}
                  (map (fn [[ks require-form]]
                         {:form require-form :ks ks :path path}))
                  (analyze-form require? (path->ks path) form))}))

(def analyze-xf
  (map (fn [[event path]]
         (if (= event :delete)
           [event path]
           [event (analyze-file path)]))))

(defn direct-ancestor?
  [[node x]]
  (let [ks (rest (:form x))
        ct (count ks)]
    (= ks (take ct (:ks node)))))

;; ------------------- compilation ----------------------

(defn add-file
  [state {:keys [path form nodes]}]
  (-> state
      (update :forms assoc path form)
      (update :graph stitch-nodes direct-ancestor? nodes)))

(defn mod-file
  [state {:keys [path form nodes]}]
  (-> state
      (update :forms assoc path form)
      (update :graph
              (fn [graph]
                (restitch-nodes graph
                                direct-ancestor?
                                (filter-nodes #(= (:path %) path) graph)
                                nodes)))))

(defn rm-file
  [{:keys [graph forms]} path]
  {:graph (->> graph
               (filter-nodes #(= (:path %) path))
               (uber/remove-nodes* graph))
   :forms (dissoc forms :path)})

(def event-fns
  {:create add-file
   :modify mod-file
   :remove rm-file})

(defn compile-directory
  [path]
  (let [state  (atom {:graph (uber/digraph) :forms {}})
        events (async/chan 1 analyze-xf)]
    (async/go-loop []
      (let [[event & args] (async/<! events)]
        (swap! state
               (fn [state-value]
                 (apply (get event-fns event) state-value args)))
        (recur)))
    [state events]))

