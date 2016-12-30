(ns web.core
  (:require [stasis.core :as stasis]))

(def pages {"/" "Hello, world!!!"})

(def app (stasis/serve-pages pages))
