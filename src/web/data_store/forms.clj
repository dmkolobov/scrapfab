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

;; the functions below are responsible for converting EDN data structures
;; to graph nodes.

(defmulti form->node (fn [x _ _] (form-type x)))

(defmethod form->node :require-form
  [[_ & req-ks :as form] path ks]
  {:path   path
   :ks     ks
   :form   form
   :req-ks req-ks})

(defmethod form->node :content-form
  [[_ content-type :as form] path ks]
  {:path         path
   :ks           ks
   :form         form
   :content-type content-type})

(defn require? [x] (contains? x :req-ks))