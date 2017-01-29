{:title "scrap!fab!"
 :arts   (require :art)
 :kit    (require :photographers)
 :photos (require :photos)}

(let [{:keys [title arts kit photos]} (:current-page context)
      print-key (fn [[k v]]
                    [:div.pure-g
                     [:div.pure-u-1-3 [:h2 k]]
                     [:div.pure-u-2-3 (pp v)]])]
  (base-layout :body
               [:div
                [:h1 title]
                [:h2 (str "url : " (:uri context))]
                [:hr]
                (print-key ["Arts" arts])
                [:hr]
                (print-key ["Kit" kit])
                [:hr]
                (print-key ["Photos" photos])
                [:hr]
                (print-key ["Context-Keys" (keys context)])]))