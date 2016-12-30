(ns web.layouts)

(defn base-layout
  "Renders the given body into an HTML hiccup structure containing all of
  the desired assets."
  [& {:keys [title body]}]
  [:html
   [:head
    [:title title]]
   [:body
    body]])