(ns web.core
  (:require [stasis.core :as stasis]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string]
            [hiccup.core :as hiccup]

            [web.render :refer [register-layout register-template render]]))

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

(defn basename
  [f ext]
  (clojure.string/replace (.getName f)
                          ext
                          ""))

(defn filename->id
  [coll-ns path]
  (keyword (name coll-ns)
           (basename (io/file path) ".edn")))

(defn slurp-collection!
  [coll-ns path]
  (let [sources (stasis/slurp-directory path #"\.edn$")
        names   (map #(filename->id coll-ns %) (keys sources))
        items   (map read-string (vals sources))]
    (zipmap names items)))

(defn scrapfab-menu
  [current-url]
  [:div.pure-menu.pure-menu-horizontal
   [:ul.pure-menu-list
    [:li.pure-menu-item [:a.pure-menu-link {:href "/"}      "Home"]]
    [:li.pure-menu-item [:a.pure-menu-link {:href "/about"} "About"]]
    [:li.pure-menu-item.pure-menu-has-children
     [:a.pure-menu-link {:href "/fab"} "Fabrication"]
     [:ul.pure-menu-children.pure-menu-allow-hover
      [:li.pure-menu-item [:a.pure-menu-link {:href "/fab/art"}         "Fine Art"]]
      [:li.pure-menu-item [:a.pure-menu-link {:href "/fab/residential"} "Residential"]]
      [:li.pure-menu-item [:a.pure-menu-link {:href "/fab/commercial"}  "Commercial"]]]]]])

(register-layout :scrapfab-base
                 (fn scrapfab-base
                   [& {:keys [title body current-url js css]}]
                   [:html
                    [:head
                     [:title title]]
                    [:body
                     [:div.pure-g
                      [:div.pure-u-1-3 [:div.scrap-logo "scrap"]]
                      [:div.pure-u-2-3 [:div.fab-logo   "fab"]]
                      [:div.pure-u-1-3]
                      [:div.pure-u-2-3 (scrapfab-menu current-url)]]

                      body

                     (for [js-url js]
                       [:script {:type "text/javascript" :src  js-url}])
                     (for [css-url css]
                       [:link {:rel "stylesheet" :href css-url}])]]))

(register-template :main
                   (fn main-view
                     [& _]
                     [:h1 "HELLO, WORLD!!"]))

(def resources
  {:js     ["/js/compiled/web.js"]

   :css ["https://unpkg.com/purecss@0.6.1/build/pure-min.css"
                 "https://unpkg.com/purecss@0.6.1/build/grids-min.css"
                 "https://unpkg.com/purecss@0.6.1/build/grids-responsive-min.css"
                 "css/main.css"
                 "css/fonts.css"]})

(def pages
  (merge {"/" (str "<!DOCTYPE html>"
                   (hiccup/html
                    (render :layout   :scrapfab-base
                            :template :main
                            :data     (merge resources
                                             {:message "foobar"}))))}

         (slurp-css!)
         (slurp-js!)))

(doall
  (map println (keys pages)))

(def app (stasis/serve-pages pages))
;;