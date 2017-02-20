(ns web.data-store.forms
  (:require [clojure.walk :as walk]))

;; ----- NODES -----------------
;; -------------------------------------------

(defprotocol IAstNode
  (emit- [_ db ctx]))

(defn emit
  "Given a node, the compiler db, and a ctx, return a new ctx with all
  instances of the node form replace with the node value."
  [{:keys [ks form] :as node} db ctx]
  (let [node-val (emit- node db ctx)]
    (update-in ctx
               ks
               (fn [ctx-val]
                 (walk/postwalk-replace {form node-val} ctx-val)))))

;; -- parsing --------------------------------------------------------
;; -------------------------------------------------------------------

(defmulti parse-form
  "Given a form and a path to the node, return the node encoded by the form."
  (fn [x ks] (first x)))

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

(defrecord RequireForm [ks form req-ks]
  IAstNode
  (emit- [_ db ctx]
    (get-in ctx req-ks)))

(defrecord ContentForm [ks form content-type]
  IAstNode
  (emit- [this {:keys [sources]} ctx]
    (let [source (get sources (:path this))]
      (render-content content-type source {}))))

(defmethod parse-form 'require
  [[_ & req-ks :as form] ks]
  (map->RequireForm
    {:ks     ks
     :form   form
     :req-ks req-ks}))

(defmethod parse-form 'content
  [[_ content-type :as form] ks]
  (map->ContentForm
    {:ks           ks
     :form         form
     :content-type content-type}))

(defn depends-on?
  [require x]
  (let [{:keys [req-ks]} require]
    (= req-ks
       (take (count req-ks) (:ks x)))))

(defmethod valid-edge? '[content require]
  [[content require]]
  (depends-on? require content))

(defmethod valid-edge? '[require require]
  [[require-a require-b]]
  (depends-on? require-b require-a))

(defmethod valid-edge? :default [_] false)