(ns web.data-store.forms)

;; how do we make this function extensible?

;; What is the scope of enhancements that this library brings in terms of
;; augmenting EDN? Should we worry about much beyond providing 'require' and
;; 'content' forms?

(defn form-type
  "Given an arbitrary sub-form from an EDN data structure, return the
  form type if it is a compiler form, and nil otherwise."
  [form]
  (when (sequential? form)
    (condp = (first form)
      'require :require-form
      'content :content-form
      nil)))

;; ----- NODES -----------------
;; -------------------------------------------

(defprotocol IAstNode
  (emit [_ db ctx]))

(defrecord RequireForm [ks form req-ks]
  IAstNode
  (emit [_ db ctx]
    (get-in ctx req-ks)))

(defrecord ContentForm [ks form content-type]
  IAstNode
  (emit [this {:keys [sources content-types]} ctx]
    (let [render (get content-types content-type)
          source (get sources (:path this))]
      (render source))))

;; ----- PARSING ---------------
;; -------------------------------------------

(defmulti form->node (fn [x _] (form-type x)))

(defmethod form->node :require-form
  [[_ & req-ks :as form] ks]
  (map->RequireForm
    {:ks     ks
     :form   form
     :req-ks req-ks}))

(defmethod form->node :content-form
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

;; We choose to implement 'valid-edge?' as a multimethod because
;; it forces us to exhaustivly define all possible dependencies
;; between any two types of nodes.
;;
;; Since the number of nodes is likely to be small, this shouldn't
;; be an issue. If this assumption changes, another method may become
;; preferrable.

(defmulti valid-edge?
          (juxt (comp type first)
                (comp type second)))

;; -- default edges --------------------------------------------------
;; -------------------------------------------------------------------

(defmethod valid-edge? [ContentForm RequireForm]
  [[content require]]
  (depends-on? require content))

(defmethod valid-edge? [RequireForm RequireForm]
  [[require-a require-b]]
  (depends-on? require-b require-a))

(defmethod valid-edge? :default
  [[_ _]]
  false)
