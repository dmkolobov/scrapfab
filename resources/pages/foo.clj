{:title "foobarcar"
:photos (pull :photos :fire_pit)}

(base-layout :body [:div
                    [:h1 (get-in context [:current-page :title])]
                    [:h2 "hello"]])