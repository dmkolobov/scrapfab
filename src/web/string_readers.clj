(ns web.string-readers
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]))

(defn with-edn-header
  [source]
  (let [reader (string-push-back-reader source)
        meta   (edn/read reader)]
    (loop [c (read-char reader) s (StringBuilder.)]
      (if (some? c)
        (recur (read-char reader) (.append s c))
        [meta (str s)]))))