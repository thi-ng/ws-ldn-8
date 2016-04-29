(ns ex06.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ex06.main :as main]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

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

(defn rtc-status
  []
  (let [status (reaction (-> @main/app :stream :state))]
    (fn []
      [:div#rtc-status "Stream status: " (name @status)])))

(defn shader-selector
  []
  (let [shaders (reaction (-> @main/app :shaders))
        curr    (reaction (-> @main/app :curr-shader))]
    (fn []
      (when @shaders
        [:select {:default-value (name @curr)
                  :on-change     #(main/set-shader! (-> % .-target .-value))}
         (for [s (sort (keys @shaders)) :let [s (name s)]]
           [:option {:key s :value s} s])]))))

(defn controls
  []
  [:div#ui
   [rtc-status]
   [shader-selector]])

(defn app-component
  []
  [:div
   [canvas-component {:init main/init-app :loop main/update-app}]
   [controls]])

(defn main
  []
  (.initializeTouchEvents js/React)
  (reagent/render-component [app-component] (dom/by-id "app")))

(main)
