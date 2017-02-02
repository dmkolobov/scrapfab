(ns web.data-store.compiler
  (:require [clojure.core.async :as async]
            [web.data-store.utils :refer [filter-nodes collect-forms stitch-nodes restitch-nodes]]
            [clojure.tools.reader.edn :as edn]
            [ubergraph.core :as uber]
            [clojure.string :as string]))

(defn drop-ext
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  [path]
  (map keyword (rest (string/split (drop-ext path) #"/"))))

(defn relative-path
  [root path]
  (string/replace path (re-pattern root) ""))

;; ------------------- analysis ----------------------

(defn form-type
  [form]
  (when (sequential? form)
    (condp = (first form)
      'require :require-form
      'content :content-form
      nil)))

(defmulti form->node (fn [x _ _] (form-type x)))

(defmethod form->node :require-form
  [form path ks]
  {:path path :ks ks :form form})

(defmethod form->node :content-form
  [[_ content-type] path ks]
  {:path path :ks ks :content-type content-type})

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
  [root path]
  (let [root-ks (path->ks (relative-path root path))
        form    (edn/read-string (slurp path))]
    {:path  path
     :form  form
     :nodes (into #{}
                  (map (fn [[ks form]] (form->node form path ks)))
                  (analyze-form form-type root-ks form))}))

(defn analyze-xf
  [root-path]
  (map (fn [[event path]]
         (if (= event :delete)
           [event path]
           [event (analyze-file root-path path)]))))

;; ------------------- compilation ----------------------

(defn direct-ancestor?
  [[node x]]
  (when (contains? x :form)
    (let [ks (rest (:form x))
          ct (count ks)]
      (= ks (take ct (:ks node))))))

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
        events (async/chan 1 (analyze-xf path))]
    (async/go-loop []
      (let [[event & args] (async/<! events)]
        (swap! state
               (fn [state-value]
                 (apply (get event-fns event) state-value args)))
        (recur)))
    [state events]))

