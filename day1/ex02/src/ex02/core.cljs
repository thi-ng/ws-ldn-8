(ns ex02.core
  (:require
   [ex02.utils :as utils]
   [ex02.shapes01]
   [ex02.shapes02]
   [thi.ng.math.core :as m]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2]]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.color.core :as col]
   [thi.ng.domus.core :as dom]
   [thi.ng.strf.core :as f]))

(enable-console-print!)

(defonce app
  (atom
   {:width  640
    :height 480
    :num    100}))

(defn make-particle
  [w h]
  {:pos    (vec2 (m/random w) (m/random h))
   :vel    (v/randvec2 (m/random 2 10))
   :radius (m/random 2 6)
   :col    @(col/as-css (col/random-rgb))})

(defn collide-particle
  [coll {:keys [pos radius] :as p}]
  (loop [coll coll]
    (if coll
      (let [q (first coll)]
        (if (identical? p q)
          (recur (next coll))
          (let [{qpos :pos qr :radius} q
                d (g/dist pos qpos)]
            (if (< d (+ radius qr))
              (let [vel (m/normalize (m/- pos qpos) (* 0.999 (m/mag (get p :vel))))
                    pos (m/+ pos (m/normalize vel (* 1.01 (- d qr))))]
                (assoc p :pos pos :vel vel))
              (recur (next coll))))))
      p)))

(defn update-particle
  [particles w h p]
  (let [;;p      (collide-particle particles p)
        {:keys [pos vel radius]} p
        [x y]   (m/+ pos vel)
        [vx vy] vel
        [x vx]  (if (< x radius)
                  [(+ radius (Math/abs vx)) (- vx)]
                  (if (> x (- w radius))
                    [(- (- w radius) (Math/abs vx)) (- vx)]
                    [x vx]))
        [y vy]  (if (< y radius)
                  [(+ radius (Math/abs vy)) (- vy)]
                  (if (> y (- h radius))
                    [(- (- h radius) (Math/abs vy)) (- vy)]
                    [y vy]))]
    (assoc p :pos (vec2 x y) :vel (vec2 vx vy))))

(defn svg-particle
  [p]
  (svg/circle (get p :pos) (get p :radius) {:fill (get p :col)}))

(defn init
  []
  (swap!
   app
   (fn [{:keys [width height num] :as state}]
     (merge state
            {:particles (vec (repeatedly num #(make-particle width height)))}))))

(defn run-sim
  [{:keys [width height num] :as state}]
  (update state
          :particles (fn [particles]
                       (reduce
                        (fn [acc i] (assoc acc i (update-particle acc width height (acc i))))
                        particles (range num)))))

(defn draw-canvas
  [canvas ctx particles]
  (set! (.-width canvas) (.-width canvas))
  (loop [particles particles]
    (when particles
      (let [{:keys [pos] :as p} (first particles)]
        (set! (.-fillStyle ctx) (get p :col))
        (doto ctx
          (.beginPath)
          (.arc (v/x pos) (v/y pos) (get p :radius) 0 m/TWO_PI)
          (.fill))
        (recur (next particles))))))

(defn main-svg
  []
  (init)
  (let [root    (dom/create! "div" (dom/by-id "app"))
        samples (volatile! [])]
    (anim/animate
     (fn [t frame]
       (let [fps (utils/run-with-timer
                  samples 30
                  #(->> root
                        (dom/clear!)
                        (dom/create-dom!
                         (svg/svg
                          {:width 640 :height 480}
                          (svg/group
                           {:stroke "none"}
                           (map svg-particle (:particles (swap! app run-sim))))))))]
         (dom/set-text!
          (dom/by-id "stats") (f/format ["SVG: " (f/float 3) " ms"] fps))
         true)))))

(defn main-svg-attribs
  []
  (init)
  (let [root    (dom/create! "div" (dom/by-id "app"))
        samples (volatile! [])
        num     (:num @app)]
    (dom/create-dom!
     (svg/svg
      {:width 640 :height 480}
      (svg/group
       {:id "particles" :stroke "none"}
       (map svg-particle (:particles (swap! app run-sim)))))
     root)
    (anim/animate
     (fn [t frame]
       (let [pel (.-children (dom/by-id "particles"))
             fps (utils/run-with-timer
                  samples 30
                  (fn []
                    (let [particles (:particles (swap! app run-sim))]
                      (loop [i 0]
                        (if (< i num)
                          (let [pos (get (nth particles i) :pos)
                                e   (aget pel i)]
                            (.setAttribute e "cx" (v/x pos))
                            (.setAttribute e "cy" (v/y pos))
                            (recur (inc i))))))))]
         (dom/set-text!
          (dom/by-id "stats") (f/format ["SVG: " (f/float 3) " ms"] fps))
         true)))))

(defn main-canvas
  []
  (init)
  (let [canvas  (dom/create-dom!
                 [:canvas {:width 640 :height 480}]
                 (dom/by-id "app"))
        ctx     (.getContext canvas "2d")
        samples (volatile! [])]
    (anim/animate
     (fn [t frame]
       (let [fps (utils/run-with-timer
                  samples 30
                  #(->> (swap! app run-sim)
                        :particles
                        (draw-canvas canvas ctx)))]
         (dom/set-text!
          (dom/by-id "stats") (f/format ["Canvas: " (f/float 3) " ms"] fps))
         true)))))

(main-svg)
;;(main-svg-attribs)
;;(main-canvas)
