(ns ex04.texture
  (:require
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.color.core :as col]
   [thi.ng.color.gradients :as grad]))

(defn gradient-texture
  [gl w h opts]
  (let [canv (.createElement js/document "canvas")
        ctx  (.getContext canv "2d")
        cols (reverse (grad/cosine-gradient h (:rainbow1 grad/cosine-schemes)))]
    (set! (.-width canv) w)
    (set! (.-height canv) h)
    (set! (.-strokeStyle ctx) "none")
    (loop [y 0, cols cols]
      (if cols
        (do
          (set! (.-fillStyle ctx) @(col/as-css (first cols)))
          (.fillRect ctx 0 y w 1)
          (recur (inc y) (next cols)))
        (buf/make-canvas-texture gl canv opts)))))
