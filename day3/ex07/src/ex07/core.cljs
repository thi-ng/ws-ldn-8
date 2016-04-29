(ns ex07.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [thi.ng.math.core :as m]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(defonce app (reagent/atom {}))

(enable-console-print!)

(defn canvas-component
  [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (reagent/set-state this {:active true})
      ((:init props) this)
      (anim/animate ((:loop props) this)))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (reagent/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width (.-innerWidth js/window)
         :height (.-innerHeight js/window)}
        props)])}))

(defn init-app
  [this]
  (let [psys        (.ccall js/Module "main")
        psys-update (.cwrap js/Module "updateParticleSystem" "*" #js ["number"])
        psys-count  (.cwrap js/Module "getNumParticles" "number" #js ["number"])
        psys-get    (.cwrap js/Module "getParticleComponent" "number" #js ["number" "number" "number"])
        canvas      (reagent/dom-node this)
        ctx         (.getContext canvas "2d")]
    (swap! app merge
           {:psys        psys
            :psys-update psys-update
            :psys-count  psys-count
            :psys-get    psys-get
            :canvas      canvas
            :ctx         ctx})))

(defn update-app
  [this]
  (fn [t frame]
    (let [{:keys [canvas ctx psys psys-update psys-count psys-get]} @app
          _   (psys-update #js [psys])
          num (psys-count #js [psys])]
      (set! (.-width canvas) (.-width canvas))
      (set! (.-fillStyle ctx) "white")
      (.fillRect ctx 500 1 10 10)
      (set! (.-fillStyle ctx) "red")
      (loop [i 0]
        (when (< i num)
          (let [x (psys-get #js [psys i 0])
                y (psys-get #js [psys i 1])]
            (doto ctx
              (.beginPath)
              (.arc x y 5 0 m/TWO_PI)
              (.fill))
            (recur (inc i)))))
      (:active (reagent/state this)))))

(defn main
  []
  (.initializeTouchEvents js/React)
  (reagent/render-component
   [canvas-component {:init init-app
                      :loop update-app}]
   (dom/by-id "app")))

(main)
