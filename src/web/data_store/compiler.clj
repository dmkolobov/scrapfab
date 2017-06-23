(ns web.data-store.compiler
  (:require [clojure.core.async :as async]
            [web.data-store.utils :refer [collect-forms stitch-nodes restitch-nodes]]
            [clojure.tools.reader.edn :as edn]
            [ubergraph.core :as uber]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [markdown.core :as md]
            [web.data-store.watches :as w]
            [web.data-store.forms :refer [valid-edge? node-ks render-content special-form? emit parse-form]]
            [ubergraph.alg :as alg]
            [clojure.walk :as walk]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]
            [clojure.pprint :refer [pprint]]))

(defn drop-ext
  [path]
  (first (string/split path #"\.")))

(defn path->ks
  [path]
  (vec (map keyword (rest (string/split (drop-ext path) #"/")))))

(defn relative-path
  [root path]
  (string/replace path (re-pattern root) ""))

;; ------------------- analysis ----------------------

(defn ks-walk
  [pred context ks form]
   (cond (pred form)
         (transduce (map-indexed (fn [idx sub-form]
                                   (ks-walk pred
                                            (into context ks)
                                            [idx]
                                            sub-form)))
                      (completing into)
                    [[form context (vec ks)]]
                    form)

         (map? form)
         (transduce (map (fn [[key sub-form]]
                           (ks-walk pred context (conj (vec ks) key) sub-form)))
                    (completing into)
                    []
                    form)

         :default
         (eduction (map #(vector % context ks)) (collect-forms pred form))))

(defn analyze-form
  [ks form]
  (eduction (map (partial apply parse-form))
            (ks-walk special-form? [] ks form)))

(defn with-edn-header
  [source]
  (let [reader (string-push-back-reader source)
        meta   (edn/read reader)]
    (loop [c (read-char reader) parts []]
      (if (some? c)
        (recur (read-char reader) (conj parts c))
        [meta (string/join parts)]))))

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
  [{:keys [nodes] :as db} path]
  (-> db
      (update :graph uber/remove-nodes* (get nodes path))
      (update :forms dissoc path)
      (update :nodes dissoc path)
      (update :sources dissoc path)))

(def event-fns
  {:create add-file
   :modify mod-file
   :remove rm-file})

;; -- default renderers ----------------------------------------------
;; -------------------------------------------------------------------

(defmethod render-content :md
  [_ source data]
  (md/md-to-html-string source))

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

(def clean-state
  {:graph    (uber/digraph)
   :forms    {}
   :nodes    {}
   :contents {}})

(defn run-compiler!
  [config]
  (let [db     (atom clean-state)
        events (watch-files! config)]
    (async/go-loop []
      (let [[event & args] (async/<! events)]
        (swap! db (fn [dbv] (apply (get event-fns event) dbv args)))
        (recur)))
    db))

