(ns web.galleries)

(defn ideal-row-count
  "Return the ideal number of rows to fill the container with items."
  [[width height :as container] aspects rows-per-screen]
  (let [ideal-height (/ height rows-per-screen)]
    (.round js/Math
            (/ (* ideal-height (reduce + aspects))
               width))))

(defn selector
  [key-fn coll]
  (let [cache (atom (group-by key-fn coll))]
    (fn [k]
      (let [item (first (get @cache k))]
        (swap! cache update k rest)
        item))))

(defn perfect-layout
  "Items are partitioned into sequences so that the sum of the aspect ratios
  in each sequence is roughly equal. Returns a sequence of sequences containing
  [id aspect-ratio] tuples.

  Arguments:

  - container       : a [width height] tuple representing the dimensions of the layout container.
  - items           : a sequence of [id aspect-ratio] tuples.
  - rows-per-screen : the ideal number of rows per screen"
  [container items rows-per-screen]
  (let [aspects    (map last items)
        partitions (js/lpartition (clj->js (map #(* 100 %) aspects))
                                  (ideal-row-count container aspects rows-per-screen))]
    (map (partial map (selector #(* 100 (last %)) items)))
         partitions))

(def sum-row-aspects (comp #(reduce + %) (map last)))

(defn row-height
  [row-width items]
  (/ (sum-row-aspects items) row-width))

(defn do-scale-layout
  [width layout gap]
  (loop [[x y]         [0 0]
         scaled-row    []
         height        (row-height width (first layout))
         row           (first layout)
         rows          (rest layout)
         scaled-layout []]
    (cond (seq row)
          (let [[[id aspect-ratio] & row'] row
                item-width                 (* aspect-ratio height)
                item-layout                [id item-width height x y]]
            (recur [(+ x item-width gap)]
                   (conj scaled-row item-layout)
                   height
                   row'
                   rows
                   scaled-layout))

          (seq rows)
          (recur [0 (+ y height gap)]
                 []
                 (row-height width (first rows))
                 (first rows)
                 (rest rows)
                 (conj scaled-layout scaled-row))

          :default
          [(conj scaled-layout scaled-row) width (+ y height)])))

(defn scale-layout
  "Returns a sequence of sequences containing [id [x y] [width height]] tuples.

  Arguments:

  - container : a [width height] tuple representing the dimensions of the layout container.
  - layout    : sequence of sequences containing [id aspect-ratio] tuples.
  - gap       : a number representing the number of pixels of whitespace
                to keep between inner layout items."
  [container layout gap]
  (do-scale-layout (first container) layout gap))