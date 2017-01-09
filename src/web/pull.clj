(ns web.pull
  (:use [clojure.walk]))

(defn pull-form? [form] (and (list? form) (= 'pull (first form))))

(defn pull
  "Recursively replace lists of the form (pull & ks) with the value of (get-in db ks)."
  [db data]
  (prewalk (fn [form]
             (if (pull-form? form)
               (pull db (get-in db (rest form)))
               form))
           data))