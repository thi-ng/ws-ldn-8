(ns ex01.faster
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [ex01.utils :as utils]
   [thi.ng.typedarrays.core :as ta]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [thi.ng.strf.core :as f]))

(defn sum-neighbors
  "Returns number of active neighbours for a cell at x;y using
  thi.ng.math macro to compute sum."
  [grid idx stride]
  (let [t (- idx stride)
        b (+ idx stride)]
    (mm/add
     (nth grid (- t 1))
     (nth grid t)
     (nth grid (+ t 1))
     (nth grid (- idx 1))
     (nth grid (+ idx 1))
     (nth grid (- b 1))
     (nth grid b)
     (nth grid (+ b 1)))))

(defn life-step
  "Computes new state for a single cell."
  [grid idx stride]
  (let [neighbors (sum-neighbors grid idx stride)]
    (if (pos? (nth grid idx))
      (if (or (== neighbors 2) (== neighbors 3)) 1 0)
      (if (== 3 neighbors) 1 0))))

(defn life
  "Computes next generation of entire cell grid."
  [w h grid]
  (let [w' (- w 1)
        h' (- h 2)]
    (loop [grid' grid, idx (+ w 1), x 1, y 1]
      (if (< x w')
        (recur (assoc grid' idx (life-step grid idx w)) (inc idx) (inc x) y)
        (if (< y h')
          (recur grid' (+ idx 2) 1 (inc y))
          grid')))))

(defn draw
  "Visualizes grid state in given canvas context."
  [ctx w h grid]
  (let [w' (- w 1)
        h' (- h 2)]
    (set! (.-fillStyle ctx) "#000")
    (.fillRect ctx 0 0 w h)
    (set! (.-fillStyle ctx) "#f00")
    (loop [i 0, x 1, y 1]
      (if (< x w')
        (do (when (pos? (nth grid i))
              (.fillRect ctx x y 1 1))
            (recur (inc i) (inc x) y))
        (if (< y h')
          (recur (+ i 2) 1 (inc y))
          grid)))))

(defn main
  [canvas ctx width height]
  (let [num     (* width height)
        grid    (->> #(if (< (rand) 0.25) 1 0)
                     (repeatedly num)
                     vec
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
