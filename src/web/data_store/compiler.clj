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

;; ---- COMPILATION ----
;; -------------------------------------------------------------------
;;
;; These functions are file system watch event handlers. They are responsible
;; for maintaining a cache of parsed EDN forms from each file,
;; as well as keeping track of dependencies introduced
;; by '(require :some :ks :to :data)' forms in the EDN forms.

(defn add-file
  [db root-ks source]
  (let [[form content] (with-edn-header source)
        nodes          (into #{} (analyze-form root-ks form))]
    (-> db
        (update :forms assoc root-ks form)
        (update :nodes assoc root-ks nodes)
        (update :sources assoc root-ks content)
        (update :graph stitch-nodes valid-edge? nodes))))

(defn mod-file
  [db root-ks source]
  (let [[form content] (with-edn-header source)
        nodes          (into #{} (analyze-form root-ks form))
        old-nodes      (get-in db [:nodes root-ks])]
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

(defn file? [path] (not (.isDirectory (io/as-file path))))

(defn file-event-xf
  [root]
  (map (fn [[event path]]
         (let [ks (path->ks (relative-path root path))]
           (if (= event :delete)
             [event ks]
             [event ks (slurp path)])))))

(defn file-event-chan
  [path]
  (async/chan 1 (comp (filter (comp file? second))
                      (file-event-xf path))))

(defn watch-files!
  [{:keys [source-paths]}]
  (let [events-chan (async/chan)
        mix         (async/mix events-chan)]
    (w/start-watch
      (map (fn [src-path]
             (let [ana-chan (file-event-chan src-path)
                   watch    (w/watch-spec ana-chan src-path)]
               (async/admix mix ana-chan)
               watch))
           source-paths))
    events-chan))

(def clean-state
  {:graph (uber/digraph) :forms {} :nodes {} :contents {}})

(defn run-compiler!
  [config]
  (let [db     (atom clean-state)
        events (watch-files! config)]
    (async/go-loop []
      (let [[event & args] (async/<! events)]
        (swap! db (fn [dbv] (apply (get event-fns event) dbv args)))
        (recur)))
    db))

