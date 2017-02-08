(ns web.data-store.compiler
  (:require [clojure.core.async :as async]
            [web.data-store.utils :refer [collect-forms stitch-nodes restitch-nodes]]
            [clojure.tools.reader.edn :as edn]
            [ubergraph.core :as uber]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [web.data-store.watches :as w]
            [web.data-store.forms :refer [form-type valid-edge? emit require? form->node]]
            [ubergraph.alg :as alg]
            [clojure.walk :as walk]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]
            [clojure.pprint :refer [pprint]]))

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

(defn filter-ks-walk
  ([pred form]
   (filter-ks-walk pred [] form))

  ([pred ks form]
   (cond (pred form)
         [[ks form]]

         (map? form)
         (transduce (map (fn [[k v]]
                           (filter-ks-walk pred (conj (vec ks) k) v)))
                    (completing into)
                    []
                    form)

         :default
         (eduction (map #(vector ks %)) (collect-forms pred form)))))

(defn analyze-form
  [ks form]
  (eduction (map (fn [[ks form]] (form->node form ks)))
            (filter-ks-walk form-type ks form)))

(defn with-edn-header
  [source]
  (let [reader (string-push-back-reader source)
        meta   (edn/read reader)]
    (loop [c (read-char reader) s (StringBuilder.)]
      (if (some? c)
        (recur (read-char reader) (.append s c))
        [meta (str s)]))))

(defn analyze-file
  "Given a root directory and a path to a file, read it's EDN header
  and find any special forms. The EDN structure will reside at the key
  sequence defined by the application of 'path->ks' to 'path', relative
  to 'root'."
  [root path]
  (let [rel-path       (relative-path root path)
        root-ks        (path->ks rel-path)
        [form content] (with-edn-header (slurp path))]
    {:path     path
     :rel-path rel-path
     :form     form
     :content  content
     :nodes    (into #{}
                     (map (fn [ast] (merge ast {:path path})))
                     (analyze-form root-ks form))}))

(defn analyze-xf
  "Returns a transformer replaces [event path] tuples with [event ast] ."
  [root-path]
  (map (fn [[event path]]
         (if (= event :delete)
           [event path]
           [event (analyze-file root-path path)]))))

;; ---- COMPILATION ----
;; -------------------------------------------------------------------
;;
;; These functions are file system watch event handlers. They are responsible
;; for maintaining a cache of parsed EDN forms from each file,
;; as well as keeping track of dependencies introduced
;; by '(require :some :ks :to :data)' forms in the EDN forms.

(defn add-file
  [db {:keys [path rel-path form nodes content]}]
  (-> db
      (update :forms assoc rel-path form)
      (update :nodes assoc path nodes)
      (update :sources assoc path content)
      (update :graph stitch-nodes valid-edge? nodes)))

(defn mod-file
  [db {:keys [path rel-path form nodes content]}]
  (let [old-nodes (get-in db [:nodes path])]
    (-> db
        (update :forms assoc rel-path form)
        (update :nodes assoc path nodes)
        (update :sources assoc path content)
        (update :graph restitch-nodes valid-edge? old-nodes nodes))))

(defn rm-file
  [{:keys [graph forms nodes sources]} path]
  {:graph    (uber/remove-nodes* graph (get nodes path))
   :forms    (dissoc forms path)
   :nodes    (dissoc nodes path)
   :sources  (dissoc sources path)})

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
  [{:keys [content-types] :as config}]
  (let [db     (atom {:graph         (uber/digraph)
                      :forms         {}
                      :nodes         {}
                      :contents      {}
                      :content-types content-types})
        events (watch-files! config)]
    (async/go-loop []
      (let [[event & args] (async/<! events)]
        (swap! db
               (fn [db-value]
                 (apply (get event-fns event) db-value args)))
        (recur)))
    db))

;; ---- EXECUTION ----------------------------------------------------

(defn fs-context
  [context [path form]]
  (assoc-in context (path->ks path) form))

(defn replace-node
  [ctx node val]
  (update-in ctx
             (:ks node)
             (fn [ctx-val]
               (walk/postwalk-replace {(:form node) val} ctx-val))))

(defn evaluate

  ([{:keys [graph] :as db}]
   (evaluate db (alg/topsort graph)))

  ([{:keys [forms] :as db} order]
   (evaluate db (reduce fs-context {} forms) order))

  ([db ctx order]
   (reduce (fn [ctx node]
             (replace-node ctx node (emit node db ctx)))
           ctx
           order)))

(defn expand
  [db]
  (evaluate @db))