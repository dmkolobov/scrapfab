(ns web.pull
  (:use [clojure.walk]
        [web.graph :refer [topsort]]))


(defn pull-form? [form] (and (list? form) (= 'pull (first form))))

(defn walk-pull
  [state ks form]
  (when (pull-form? form)
    (swap! state conj [ks form]))
  form)

(defn collect-pulls-
  [state ks form]
  (if (map? form)
    (doall
      (map (fn [[k v]] (collect-pulls- state (conj ks k) v))
         form))
    (prewalk #(walk-pull state ks %) form))) ;; allows pull cycles in non-map values.

(defn collect-pulls
  [form]
  (let [state (atom [])] (collect-pulls- state [] form) @state))

(defn get-pulls
  [form]
  (let [pulls (collect-pulls form)]
    (mapcat (fn [pull]
              (let [pull-ks (rest pull)
                    pull-ct (count pull-ks)]
                (if-let [deps (seq
                                (->> pulls
                                    (filter #(= pull-ks (take pull-ct (first %))))
                                    (map second)
                                    (map #(vector pull %))))]
                  deps
                  [[pull nil]])))
            (into #{} (map second pulls)))))

(def analyze (comp topsort get-pulls))

(defn compile
  [form]
  (let [smap  (reduce (fn [smap pull]
                        (let [pull-form (get-in form (rest pull))]
                          (assoc smap
                            pull (prewalk-replace smap pull-form))))
                      {}
                      (analyze form))]
    (prewalk-replace smap form)))

(defn pull
  [context form]
  (prewalk (fn [x]
             (if (pull-form? x)
               (get-in context (rest x))
               x))
           form))