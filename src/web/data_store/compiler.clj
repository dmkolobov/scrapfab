(ns web.data-store.compiler
  (:require [clojure.core.async :as async]
            [web.data-store.utils :refer [filter-nodes collect-forms stitch-nodes restitch-nodes]]
            [clojure.tools.reader.edn :as edn]
            [ubergraph.core :as uber]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [web.data-store.watches :as w]
            [web.data-store.forms :refer [form-type require? form->node]]))

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
  (when (require? x)
    (let [{:keys [req-ks]} x
          ct     (count req-ks)]
      (= req-ks (take ct (:ks node))))))

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

;; ------------------- file watching ----------------------

(defn file?
  [path]
  (let [f (io/as-file path)]
    (not (.isDirectory f))))

(defn analysis-chan
  [path]
  (async/chan 1 (comp (filter (comp file? second))
                      (analyze-xf path))))

(defn watch-files!
  [{:keys [source-paths]}]
  (let [events-chan (async/chan)
        mix         (async/mix events-chan)]
    (w/start-watch
      (map (fn [src-path]
             (let [ana-chan (analysis-chan src-path)
                   watch    (w/watch-spec ana-chan src-path)]
               (async/admix mix ana-chan)
               watch))
           source-paths))
    events-chan))

(defn run-compiler!
  [config]
  (let [state  (atom {:graph (uber/digraph) :forms {}})
        events (watch-files! config)]
    (async/go-loop []
      (let [[event & args] (async/<! events)]
        (swap! state
               (fn [state-value]
                 (apply (get event-fns event) state-value args)))
        (recur)))
    state))
