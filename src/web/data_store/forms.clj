(ns web.data-store.forms
  (:require [clojure.walk :as walk]))

;; ----- NODES -----------------
;; -------------------------------------------

(defprotocol IAstNode
  (emit- [_ db ctx]))

(defn load-node
  [contexts node]
  (if-let [node-value (get contexts (into (:context node) (:ks node)))]
    (assoc node :form node-value)
    node))

(defn emit
  [{:keys [ks context form] :as node} db contexts]
  (update contexts
          context
          update-in
          ks
          (fn [result-fragment]
            (let [val (emit- (load-node contexts node) db contexts)]
              (walk/postwalk-replace {form val} result-fragment)))))

;; -- parsing --------------------------------------------------------
;; -------------------------------------------------------------------

(defmulti parse-form
  "Given a form and a path to the node, return the node encoded by the form."
  (fn [x context ks] (first x)))

(defn special-forms
  "Returns the set of all dispatch values of 'parse-form'."
  []
  (into #{} (keys (methods parse-form))))

(defn special-form?
  "Returns true if 'form' is sequential and its first element is a registered
  dispatch value of 'parse-form'."
  [form]
  (and (sequential? form)
       (contains? (special-forms) (first form))))

;; -- edges ----------------------------------------------------------
;; -------------------------------------------------------------------

(defmulti special-edge?
  (juxt (comp first :form first)
        (comp first :form second)))

(defmulti render-content
  (fn [content-type source data] content-type))

;; -- default edges --------------------------------------------------
;; -------------------------------------------------------------------

(defn node-ks
  "Returns the full key sequence pointing to the node."
  [node]
  (into (vec (:context node)) (:ks node)))

(defrecord RequireForm [ks context form require-ks]
  IAstNode
  (emit- [_ db contexts] (get-in (get contexts []) require-ks)))

(defrecord ContentForm [ks context form]
  IAstNode
  (emit- [this {:keys [sources]} contexts]
    (let [source           (get sources (:path this))
          [_ content-type] form]
      (render-content content-type source {}))))

(defmethod parse-form 'require
  [[_ & require-ks :as form] context ks]
  (map->RequireForm
    {:ks         ks
     :context    context
     :form       form
     :require-ks require-ks}))

(defmethod parse-form 'content
  [[_ content-type :as form] context ks]
  (map->ContentForm
    {:ks           ks
     :context      context
     :form         form
     :content-type content-type}))

(defrecord RenderForm [ks form content-type]
  IAstNode
  (emit- [this {:keys [sources]} result]
    {:form   (:form this)
     :source (get sources (:path this))}))

(defmethod parse-form 'render
  [form context ks]
  (map->RenderForm
    {:ks ks
     :context context
     :form form}))

(defn sub-seq?
  "Returns true if u is a sub-sequence of v."
  [u v]
  (= u (take (count u) v)))

(defn parent?
  "Returns true if node x is a parent of node y."
  [x y]
  (sub-seq? (node-ks x) (node-ks y)))

(defn requires?
  "Returns true if node x requires node y."
  [x y]
  (when-let [rks (seq (:require-ks x))]
    (sub-seq? rks (node-ks y))))

(defmethod special-edge? '[content require]
  [[content require]]
  (requires? require content))

(defmethod special-edge? '[require require]
  [[require-a require-b]]
  (requires? require-b require-a))

(defmethod special-edge? '[render require]
  [[render require]]
  (requires? require render))

(defmethod special-edge? :default [_] false)

(defn valid-edge?
  [[from to :as edge]]
  (and (not= from to)
       (or (parent? to from)
           (special-edge? edge))))