(ns web.pull
  (:use [clojure.walk]
        [clojure.set :refer [difference]]))


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

(defn assoc-set
  [m [v dep]]
  (update m v (fn [x] (if x (conj x dep) (if dep #{dep} #{})))))

(def free-xf
  (comp (filter (comp empty? second)) (map first)))

(defn remove-free
  [deps free-set]
  (reduce (fn [deps [v v-deps]]
            (assoc deps v (difference v-deps free-set)))
          {}
          (apply dissoc deps free-set)))

(defn topsort
  [edges]
  (loop [order []
         deps  (reduce assoc-set {} edges)]
    (if (seq deps)
      (let [free-set (into #{} free-xf deps)]
        (if (seq free-set)
          (recur (into order free-set) (remove-free deps free-set))
          (throw (Error. "No topological sort found. A cycle is present."))))
      order)))

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