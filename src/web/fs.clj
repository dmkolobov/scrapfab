(ns web.fs)

(defn get-path [file] (.getPath file))
(defn get-name [file] (.getName file))

(defn get-relative-path
  [base file]
  (loop [b (clojure.string/split base #"/")
         p (clojure.string/split (get-path file) #"/")]
    (if (seq b)
      (recur (rest b)
             (rest p))
      (clojure.string/join "/" p))))
