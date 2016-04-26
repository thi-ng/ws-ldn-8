(ns ex01.fastest
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
     (aget grid (- t 1))
     (aget grid t)
     (aget grid (+ t 1))
     (aget grid (- idx 1))
     (aget grid (+ idx 1))
     (aget grid (- b 1))
     (aget grid b)
     (aget grid (+ b 1)))))

(defn life-step
  "Computes new state for a single cell."
  [grid idx stride]
  (let [neighbors (sum-neighbors grid idx stride)]
    (if (pos? (aget grid idx))
      (if (or (== neighbors 2) (== neighbors 3)) 1 0)
      (if (== neighbors 3) 1 0))))

(defn life
  "Computes next generation of entire cell grid."
  [w h [old new]]
  (let [w' (- w 1)
        h' (- h 2)]
    (loop [idx (+ w 1), x 1, y 1]
      (if (< x w')
        (do
          (aset new idx (life-step old idx w))
          (recur (inc idx) (inc x) y))
        (if (< y h')
          (recur (+ idx 2) 1 (inc y))
          [new old])))))

(defn draw
  "Visualizes grid state in given canvas context & image data buffer."
  [ctx img len [grid :as state]]
  (let [pixels (.-data img)]
    (loop [i 0, idx 0]
      (if (< i len)
        (do (aset pixels idx (* (aget grid i) 0xff))
            (recur (inc i) (+ idx 4)))
        (do (.putImageData ctx img 0 0)
            state)))))

(defn prepare-image
  "Creates an ImageData object for given canvas context and fills it
  with opaque black."
  [ctx width height]
  (let [img (.createImageData ctx width height)]
    (.fill (-> img .-data .-buffer js/Uint32Array.) (int 0xff000000))
    img))

(defn main
  [canvas ctx width height]
  (let [num     (* width height)
        grid    (->> #(if (< (rand) 0.5) 1 0)
                     (repeatedly num)
                     ta/uint8)
        grid2   (ta/uint8 num)
        img     (prepare-image ctx width height)
        state   (volatile! [grid grid2])
        samples (volatile! [])
        stats   (dom/by-id "stats")]
    (anim/animate
     (fn [_ _]
       (let [avg (utils/run-with-timer
                  samples 30
                  (fn []
                    (vswap! state
                            #(->> %
                                  (life width height)
                                  (draw ctx img num)))))]
         (dom/set-text! stats (f/format [(f/float 3) " ms"] avg))
         true)))))
