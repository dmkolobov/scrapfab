(ns web.data-store.forms
  (:require [clojure.walk :as walk]
            [clojure.string :refer [capitalize]]))

;; ----- NODES -----------------
;; -------------------------------------------

(defprotocol IAstNode
  (value [_ db ctx])
  (depends? [this node]))


(defn node-name
  [sym]
  (symbol (str (capitalize sym) "Node")))

(defn node-constructor
  [sym]
  (symbol (str "map->" (node-name sym))))

(defn create-node-record
  ([sym value]
   (create-node-record sym value `~'(depends? [_ _] false)))
  ([sym value depends?]
   `(do
      (defrecord ~(node-name sym) [~'context ~'ks ~'form]
        IAstNode
        ~value
        ~depends?)

      (defmethod parse-form '~sym
        [~'form ~'context ~'ks]
        (~(node-constructor sym)
          ~'{:form    form
             :context context
             :ks      ks})))))

(defmacro defnode
  [sym & args]
  (apply create-node-record sym args))

(defn node-ks
  "Returns the full key sequence pointing to the node."
  [node]
  (into (vec (:context node)) (:ks node)))

(defn load-node
  [state node]
  (if-let [ctx-state (get state (node-ks node))]
    (assoc node :form ctx-state)
    node))

(defn emit
  [{:keys [ks context form] :as node} db state]
  (update state
          context
          update-in
          ks
          (fn [result-fragment]
            (let [val (value (load-node state node) db state)]
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

(defmulti render-content
  (fn [content-type source data] content-type))

;; -- default edges --------------------------------------------------
;; -------------------------------------------------------------------

(defn sub-seq?
  "Returns true if u is a sub-sequence of v."
  [u v]
  (= u (take (count u) v)))

(defn parent?
  "Returns true if node x is a parent of node y."
  [x y]
  (sub-seq? (node-ks x) (node-ks y)))

(defn valid-edge?
  [[from to :as edge]]
  (and (not= from to)
       (or (parent? to from)
           (depends? to from))))

;; -------- default node implementations

(defnode require
         (value [_ db state]
           (get-in (get state []) (rest form)))

         (depends? [this node]
           (sub-seq? (rest form) (node-ks node))))

(defnode content
         (value [this {:keys [sources]} state]
           (let [source (get sources (:path this))
                 content-type (last form)]
             (render-content content-type source {}))))

(defnode render
         (value [this {:keys [sources]} state]
           {:form   form
            :source (get sources (:path this))}))
