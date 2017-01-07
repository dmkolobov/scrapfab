(ns web.ui)

(defn main-layout
  [render {:keys [title content js css]}]
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

    (doall
      (for [js-url js]
      [:script {:type "text/javascript" :src  js-url}]))
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
