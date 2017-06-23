(ns web.data-store.compiler
  (:require [clojure.core.async :as async]
            [web.data-store.utils :refer [collect-forms stitch-nodes restitch-nodes]]
            [clojure.tools.reader.edn :as edn]
            [ubergraph.core :as uber]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [markdown.core :as md]
            [web.data-store.watches :as w]
            [web.data-store.forms :refer [valid-edge? analyze-form node-ks render-content special-form? emit parse-form]]
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
  [root-ks source]
  (let [[form content] (with-edn-header source)]
    {:root-ks  root-ks
     :form     form
     :content  content
     :nodes    (into #{} (analyze-form root-ks form))}))

(defn analyze-xf
  "Returns a transformer replaces [event path] tuples with [event ast] ."
  [root]
  (map (fn [[event path]]
         (let [ks (path->ks (relative-path root path))]
           (if (= event :delete)
             [event ks]
             [event (analyze-file ks (slurp path))])))))

;; ---- COMPILATION ----
;; -------------------------------------------------------------------
;;
;; These functions are file system watch event handlers. They are responsible
;; for maintaining a cache of parsed EDN forms from each file,
;; as well as keeping track of dependencies introduced
;; by '(require :some :ks :to :data)' forms in the EDN forms.

(defn add-file
  [db {:keys [root-ks form nodes content]}]
  (-> db
      (update :forms assoc root-ks form)
      (update :nodes assoc root-ks nodes)
      (update :sources assoc root-ks content)
      (update :graph stitch-nodes valid-edge? nodes)))

(defn mod-file
  [db {:keys [root-ks form nodes content]}]
  (let [old-nodes (get-in db [:nodes root-ks])]
    (-> db
        (update :forms assoc root-ks form)
        (update :nodes assoc root-ks nodes)
        (update :sources assoc root-ks content)
        (update :graph restitch-nodes valid-edge? old-nodes nodes))))

(defn rm-file
  [{:keys [nodes] :as db} root-ks]
  (-> db
      (update :graph uber/remove-nodes* (get nodes root-ks))
      (update :forms dissoc root-ks)
      (update :nodes dissoc root-ks)
      (update :sources dissoc root-ks)))

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

