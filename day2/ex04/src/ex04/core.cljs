(ns ex04.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ex04.game :as game]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(enable-console-print!)

(defn gl-component
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

(defn main
  []
  (.initializeTouchEvents js/React)
  (let [root [gl-component
              {:init          game/init-game
               :loop          game/game-loop
               :on-mouse-move (fn [e] (game/update-player-pos! (.-clientX e)))
               :on-mouse-down (fn [e] (game/player-max-speed!))
               :on-mouse-up   (fn [e] (game/player-normal-speed!))
               :on-touch-move (fn [e]
                                (game/update-player-pos! (.-clientX (aget (.-touches e) 0)))
                                (game/player-max-speed!))
               :on-touch-end  (fn [e] (game/player-normal-speed!))}]]
    (reagent/render-component root (dom/by-id "app"))))

(main)
