{:title "scrap!fab!"
 :arts   (pull :art)
 :kit    (pull :photographers)
 :photos (pull :photos)}

(base-layout :body [:div
                    [:h1 (get-in context [:current-page :title])]
                    [:h2 "foobar"]])