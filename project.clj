(defproject web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [stasis "2.3.0"]
                 [ring "1.5.0"]]
  :plugins [[lein-ring "0.10.0"]]
  :main ^:skip-aot web.core
  :target-path "target/%s"
  :ring {:handler web.core/app}
  :profiles {:uberjar {:aot :all}})
