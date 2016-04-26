(ns ex01.naive
  (:require
   [ex01.utils :as utils]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [thi.ng.strf.core :as f]))

(defn sum-neighbors
  "Returns number of active neighbours for a cell at x;y using
  standard map/reduce."
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (dec y)
        +y (inc y)]
    (->> [[-y -x] [-y x] [-y +x]
          [y -x]         [y +x]
          [+y -x] [+y x] [+y +x]]
         (map #(get-in grid %))
         (reduce +))))

(defn sum-neighbors-transduce
  "Returns number of active neighbours for a cell at x;y using transduce."
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (dec y)
        +y (inc y)]
    (transduce
     (map #(get-in grid %))
     +
     [[-y -x] [-y x] [-y +x]
      [ y -x]        [ y +x]
      [+y -x] [+y x] [+y +x]])))

(defn sum-neighbors-no-reduce
  "Returns number of active neighbours for a cell at x;y using
  flattened cell lookups w/o reduce."
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (dec y)
        +y (inc y)]
    (+ (+ (+ (+ (+ (+ (+ (get-in grid [-y -x])
                         (get-in grid [-y x]))
                      (get-in grid [-y +x]))
                   (get-in grid [y -x]))
                (get-in grid [y +x]))
             (get-in grid [+y -x]))
          (get-in grid [+y x]))
       (get-in grid [+y +x]))))

(defn sum-neighbors-nth
  "Returns number of active neighbours for a cell at x;y using
  flattened cell lookups and avoiding use of get-in."
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (nth grid (dec y))
        +y (nth grid (inc y))
        y  (nth grid y)]
    (+ (+ (+ (+ (+ (+ (+ (nth -y -x)
                         (nth -y x))
                      (nth -y +x))
                   (nth y -x))
                (nth y +x))
             (nth +y -x))
          (nth +y x))
       (nth +y +x))))

(defn life-step
  "Computes new state for a single cell."
  [grid x y state]
  (let [neighbors (sum-neighbors grid x y) ;; 594ms (advanced compile, else x2)
        ;;neighbors (sum-neighbors-transduce grid x y) ;; 780ms
        ;;neighbors (sum-neighbors-no-reduce grid x y) ;; 270ms
        ;;neighbors (sum-neighbors-nth grid x y) ;; 128ms
        ]
    (if (pos? state)
      (if (or (== neighbors 2) (== neighbors 3)) 1 0)
      (if (== 3 neighbors) 1 0))))

(defn life
  "Computes next generation of entire cell grid."
  [w h grid]
  (let [w' (- w 1)
        h' (- h 2)]
    (loop [grid' grid, y 1, x 1]
      (if (< x w')
        (recur (update-in grid' [y x] #(life-step grid x y %)) y (inc x))
        (if (< y h')
          (recur grid' (inc y) 1)
          grid')))))

(defn draw
  "Visualizes grid state in given canvas."
  [ctx w h grid]
  (let [w' (- w 1)
        h' (- h 2)]
    (set! (.-fillStyle ctx) "#000")
    (.fillRect ctx 0 0 w h)
    (set! (.-fillStyle ctx) "#f00")
    (loop [y 1, x 1]
      (if (< x w)
        (do (when (pos? (get-in grid [y x]))
              (.fillRect ctx x y 1 1))
            (recur y (inc x)))
        (if (< y h')
          (recur (inc y) 1)
          grid)))))

(defn main
  [canvas ctx width height]
  (let [grid    (->> #(if (< (rand) 0.25) 1 0)
                     (repeatedly (* width height))
                     (partition width)
                     (mapv vec)
                     volatile!)
        samples (volatile! [])
        stats   (dom/by-id "stats")]
    (anim/animate
     (fn [_ _]
       (let [avg (utils/run-with-timer
                  samples 30
                  (fn []
                    (vswap! grid
                            #(->> %
                                  (life width height)
                                  (draw ctx width height)))))]
         (dom/set-text! stats (f/format [(f/float 3) " ms"] avg))
         true)))))
