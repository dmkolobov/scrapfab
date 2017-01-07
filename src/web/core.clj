(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [hiccup.core :as hiccup]

            [web.render :refer [register-layout render-page]]))

(defn slurp-css!
  []
  (let [css (stasis/slurp-directory "resources/public/css" #"\.css$")]
    (rename-keys css
                 (zipmap (keys css)
                         (map #(str "/css" %) (keys css))))))

(defn slurp-js!
  []
  (let [js (stasis/slurp-directory "resources/public/js/compiled/" #"\.js$")]
    (rename-keys js
                 (zipmap (keys js)
                         (map #(str "/js/compiled" %) (keys js))))))

(defn scrapfab-menu
  [current-url]
  [:div.pure-menu.pure-menu-horizontal
   [:ul.pure-menu-list
    [:li.pure-menu-item [:a.pure-menu-link {:href "/"}      "Home"]]
    [:li.pure-menu-item [:a.pure-menu-link {:href "/about"} "About"]]
    [:li.pure-menu-item.pure-menu-has-children.pure-menu-allow-hover
     [:a.pure-menu-link {:href "/fab"} "Fabrication"]
     [:ul.pure-menu-children
      [:li.pure-menu-item [:a.pure-menu-link {:href "/fab/art"}         "Fine Art"]]
      [:li.pure-menu-item [:a.pure-menu-link {:href "/fab/residential"} "Residential"]]
      [:li.pure-menu-item [:a.pure-menu-link {:href "/fab/commercial"}  "Commercial"]]]]]])

(register-layout :scrapfab-base
                 (fn scrapfab-base
                   [& {:keys [title content current-url js css]}]
                   [:html
                    [:head
                     [:title title]]
                    [:body
                     [:div.pure-g
                      [:div.pure-u-1-3 [:div.scrap-logo "scrap"]]
                      [:div.pure-u-2-3 [:div.fab-logo   "fab"]]
                      [:div.pure-u-1-3]
                      [:div.pure-u-2-3 (scrapfab-menu current-url)]]

                      content

                     (for [js-url js]
                       [:script {:type "text/javascript" :src  js-url}])
                     (for [css-url css]
                       [:link {:rel "stylesheet" :href css-url}])]]))

(def resources
  {:js     ["/js/compiled/web.js"]

   :css ["https://unpkg.com/purecss@0.6.1/build/pure-min.css"
                 "https://unpkg.com/purecss@0.6.1/build/grids-min.css"
                 "https://unpkg.com/purecss@0.6.1/build/grids-responsive-min.css"
                 "/css/main.css"
                 "/css/fonts.css"]})

(defn- render
  [page]
  (render-page (merge resources page)))

(def pages
  (merge {"/"                           (render
                                          {:layout  :scrapfab-base
                                           :title   "root"
                                           :content [:h1 "Hello, World!"]})

          "/fab/art/index.html"         (render
                                          {:layout  :scrapfab-base
                                           :title   "SCRAPFAB fabrication > fine arts"
                                           :content [:h1 "Fine Art"]})

          "/fab/residential/index.html" (render
                                          {:layout  :scrapfab-base
                                           :title   "SCRAPFAB fabrication > residential"
                                           :content [:h1 "Residential"]})

          "/fab/commercial/index.html"  (render
                                          {:layout  :scrapfab-base
                                           :title   "SCRAPFAB fabrication > commercial"
                                           :content [:h1 "Commercial"]})}

         (slurp-css!)
         (slurp-js!)))

(doall
  (map println (keys pages)))

(def app (stasis/serve-pages pages))
;;