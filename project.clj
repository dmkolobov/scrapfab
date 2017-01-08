(defproject web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [org.clojure/tools.reader "1.0.0-beta4"]

                 [org.clojure/core.async "0.2.385"
                  :exclusions [org.clojure/tools.reader]]
                 [reagent "0.6.0-rc"]
                 [re-frame "0.8.0"]
                 [stasis "2.3.0"]
                 [ring "1.5.0"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-ring "0.10.0"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]



  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src-cljs"]

                :compiler {:main       web.core
                           :asset-path "/js/compiled/out"
                           :output-to  "resources/public/js/compiled/web.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}]}


  :main ^:skip-aot web.core
  :target-path "target/%s"
  :ring {:handler web.core/app}
  :profiles {:uberjar {:aot :all}})
