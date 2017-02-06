(ns web.string-readers
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader read-char]]))
