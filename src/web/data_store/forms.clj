(ns web.data-store.forms
  (:require [clojure.walk :as walk]))

;; ----- NODES -----------------
;; -------------------------------------------

(defprotocol IAstNode
  (emit- [_ db ctx]))

(defn load-node
  [contexts node]
  (if-let [node-value (get contexts (:ks node))]
    (assoc node :form node-value)
    node))

(defn emit
  [{:keys [ks context form] :as node} db contexts]
  (update contexts
          context
          (fn [result]
            (update-in result
                       ks
                       (fn [result-fragment]
                         (let [val (emit- (load-node contexts node) db contexts)]
                           (walk/postwalk-replace {form val} result-fragment)))))))

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

(defmulti valid-edge?
  (juxt (comp first :form first)
        (comp first :form second)))

(defmulti render-content
  (fn [content-type source data] content-type))

;; -- default edges --------------------------------------------------
;; -------------------------------------------------------------------

(defrecord RequireForm [ks context form req-ks]
  IAstNode
  (emit- [_ db contexts] (get-in (get contexts []) req-ks)))

(defrecord ContentForm [ks context form content-type]
  IAstNode
  (emit- [this {:keys [sources]} contexts]
    (let [source (get sources (:path this))]
      (render-content content-type source {}))))

(defmethod parse-form 'require
  [[_ & req-ks :as form] context ks]
  (map->RequireForm
    {:ks       ks
     :context  context
     :form     form
     :req-ks   req-ks}))

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
    {:data   (second form)
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

(defn depends-on?
  "Returns true if node x depends on node y."
  [x y]
  (or (sub-seq? (:req-ks x) (into (:context y) (:ks y)))
      (sub-seq? (:ks x) (:context y))))

(defmethod valid-edge? '[content require]
  [[content require]]
  (depends-on? require content))

(defmethod valid-edge? '[require require]
  [[require-a require-b]]
  (depends-on? require-b require-a))

(defmethod valid-edge? '[require render]
  [[require render]]
  (depends-on? render require))

(defmethod valid-edge? '[render require]
  [[render require]]
  (depends-on? require render))

(defmethod valid-edge? '[content render]
  [[content render]]
  (depends-on? render content))

(defmethod valid-edge? :default [_] false)