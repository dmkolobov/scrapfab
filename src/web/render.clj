(ns web.render)

(def registry (atom {:layouts   {}
                     :templates {}}))

(defn register-layout
  [id f]
  (swap! registry assoc-in [:layouts id] f))

(defn register-template
  [id f]
  (swap! registry assoc-in [:templates id] f))

(defn render
  [& {:keys [layout template data]
      :or   {data {}}}]
  (let [reg      @registry
        layout   (get-in reg [:layouts layout])
        template (get-in reg [:templates template])]
    (apply layout (mapcat identity (assoc data :body (template data))))))