(ns web.scrapfab
  (:require [clojure.pprint :refer [pprint]]
            [web.ui :refer [main-layout main-nav]]))

(defn debug-view
  [render context]
  (render :main-layout
          (assoc context
            :content [:div
                       [:h1 "Data"]
                       [:pre
                        {:data-lang "clojure"}
                        (with-out-str (pprint context))]])))

(def theme
  {:js  ["https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.22.0/codemirror.js"
         "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.22.0/mode/clojure/clojure.js"
         "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.22.0/addon/runmode/runmode.js"
         "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.22.0/addon/runmode/colorize.js"

         "/js/compiled/web.js"]

   :css ["https://unpkg.com/purecss@0.6.1/build/pure-min.css"
         "https://unpkg.com/purecss@0.6.1/build/grids-min.css"
         "https://unpkg.com/purecss@0.6.1/build/grids-responsive-min.css"

         "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.22.0/codemirror.min.css"
         "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.22.0/theme/base16-dark.css"

         "/css/main.css"
         "/css/fonts.css"]

   :layouts {:debug-view  debug-view
             :main-layout main-layout
             :main-nav    main-nav}})

(def site
  {"/"                           {:layout :debug-view
                                  :title  "Welcome"
                                  :md     "# SCRAPFAB"}

   "/fab/art/index.html"         {:layout :debug-view
                                  :title  "SCRAPFAB fabrication > fine arts"
                                  :md     "# fine arts"}

   "/fab/residential/index.html" {:layout :debug-view
                                  :title  "SCRAPFAB fabrication > residential"
                                  :md     "# residential"}

   "/fab/commercial/index.html"  {:layout :debug-view
                                  :title  "SCRAPFAB fabrication > commercial"
                                  :md     "# commercial"}})