(ns web.scrapfab)

(defn main-layout
  [render {:keys [title content url js css]}]
  [:html
   [:head
    [:title title]]
   [:body
    [:div.pure-g
     [:div.pure-u-1-3 [:div.scrap-logo "scrap"]]
     [:div.pure-u-2-3 [:div.fab-logo   "fab"]]
     [:div.pure-u-1-3]
     [:div.pure-u-2-3 (render :main-nav)]]

    content

    (for [js-url js]
      [:script {:type "text/javascript" :src  js-url}])
    (for [css-url css]
      [:link {:rel "stylesheet" :href css-url}])]])

(defn main-nav
  [render {:keys [url]}]
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

(def theme
  {:js  ["/js/compiled/web.js"]

   :css ["https://unpkg.com/purecss@0.6.1/build/pure-min.css"
         "https://unpkg.com/purecss@0.6.1/build/grids-min.css"
         "https://unpkg.com/purecss@0.6.1/build/grids-responsive-min.css"
         "/css/main.css"
         "/css/fonts.css"]

   :layouts {:main-layout main-layout
             :main-nav    main-nav}})

(def site
  {"/"                           {:layout  :main-layout
                                  :title   "root"
                                  :content [:h1 "Hello, World!!!!!!!! 6!!66"]}

   "/fab/art/index.html"         {:layout  :main-layout
                                  :title   "SCRAPFAB fabrication > fine arts"
                                  :content [:h1 "Fine Art"]}

   "/fab/residential/index.html" {:layout  :main-layout
                                  :title   "SCRAPFAB fabrication > residential"
                                  :content [:h1 "Residential"]}

   "/fab/commercial/index.html"  {:layout  :main-layout
                                  :title   "SCRAPFAB fabrication > commercial"
                                  :content [:h1 "Commercial"]}})