(ns ex01.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ex01.state :as state]
   [ex01.naive :as naive]
   [ex01.faster :as faster]
   [ex01.fastest :as fastest]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.strf.core :as f]
   [reagent.core :as r]))

(def modes
  {:naive   {:init naive/init   :update naive/redraw}
   :faster  {:init faster/init  :update faster/redraw}
   :fastest {:init fastest/init :update fastest/redraw}})

(defn update-sim
  [this props]
  (fn [_ _]
    (let [{:keys [mode size]} @state/app
          {:keys [init update]} (modes mode)
          props (assoc props :width size :height size)
          canvas (r/dom-node this)]
      (when (:reset @state/app)
        (debug :reset mode)
        (init this props)
        (swap! state/app dissoc :reset))
      (set! (.-width canvas) size)
      (set! (.-height canvas) size)
      (update this props)
      (:active (r/state this)))))

(defn canvas-component
  [props]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (r/set-state this {:active true})
      ((:init props) this props)
      (anim/animate (update-sim this props)))
    :component-will-unmount
    (fn [this]
      (debug "unmount canvas")
      (r/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width (.-innerWidth js/window)
         :height (.-innerHeight js/window)}
        props)])}))

(defn mode-selector
  []
  (let [mode (reaction (:mode @state/app))]
    (fn []
      [:select {:default-value @mode :on-change state/set-mode!}
       (for [m ["naive" "faster" "fastest"]]
         [:option {:key m :value m} m])])))

(defn size-selector
  []
  (let [size (reaction (:size @state/app))]
    (fn []
      [:select {:default-value @size :on-change state/set-size!}
       (for [m [256 512 1024]]
         [:option {:key (str m) :value m} m])])))

(defn stats
  []
  (let [avg (reaction (:avg @state/app))]
    (fn [] [:span (f/format [(f/float 3) " ms"] @avg)])))

(defn app-component
  []
  (let [mode (reaction (:mode @state/app))
        size (reaction (:size @state/app))]
    (fn []
      (let [props (assoc (modes @mode) :width @size :height @size)]
        [:div
         [:div
          [mode-selector]
          [size-selector]
          [stats]]
         [canvas-component props]]))))

(defn main
  []
  (r/render-component
   [app-component]
   (.getElementById js/document "app")))

(main)
