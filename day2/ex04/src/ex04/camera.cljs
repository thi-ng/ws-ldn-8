(ns ex04.camera
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.utils :as gu]))

(defn camera-path
  [points frames]
  (let [up (nth frames 2)]
    {:pos       points
     :pos-index (gu/arc-length-index points)
     :up        up
     :up-index  (gu/arc-length-index up)}))

(defn camera-at-path-pos
  [{:keys [pos pos-index up up-index]} t delta view-rect]
  (let [t      (mod t 1.0)
        t      (if (neg? t) (inc t) t)
        t'     (mod (+ t delta) 1.0)
        eye    (gu/point-at t pos pos-index)
        target (gu/point-at t' pos pos-index)
        up     (m/normalize (gu/point-at t up up-index))]
    (cam/perspective-camera
     {:eye    eye
      :target target
      :up     up
      :fov    90
      :aspect view-rect
      :far    10})))
