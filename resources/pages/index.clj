{:title "scrap!fab!!!"
 :art    (pull :art)
 :kit    (pull :photographers)
 :photos (pull :photos)}

(base-layout :body [:div
                    [:h1 (get-in site [:current-page :title])]
                    [:pre (with-out-str (pprint site))]])