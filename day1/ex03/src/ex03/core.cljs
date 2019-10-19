(ns ex03.core
  (:require [ex03.webgl01 :as gl01]
            [ex03.webgl02 :as gl02]
            [ex03.webgl03 :as gl03]
            [ex03.webgl04 :as gl04]
            [ex03.webgl05 :as gl05]
            [ex03.webgl06 :as gl06]
            [ex03.webgl07 :as gl07]
            [reagent.core :as r]
            [goog.dom :as gdom]))

(def demos {"Gears" gl01/demo
            "Rotating Cube" gl02/demo
            "Rotating Cube with Lighting" gl03/demo
            "Translucent Cube" gl04/demo
            "Nested Knot" gl05/demo
            "Trance" gl06/demo
            "Spinning World" gl07/demo})

(def current-demo (r/atom "gears"))

(defn canvas-component
  [demo-fn]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (r/set-state this {:active true})
      (js/console.log (r/dom-node this))
      (demo-fn (r/dom-node this)))
    :component-will-unmount
    (fn [this]
      (r/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width (.-innerWidth js/window)
         :height (.-innerHeight js/window)})])}))

(defn demo-selector []
  [:select {:default-value (name @current-demo)
            :on-change     #(reset! current-demo (-> % .-target .-value))}
   (for [s (sort (keys demos))
         :let [s (name s)]]
     [:option {:key s :value s} s])])

(defn main-component []
  (let [demo @current-demo]
    [:div
     [demo-selector]
     [(canvas-component (demos demo))]]))

(defn main []
  (let [demo @current-demo]
    (r/render-component [main-component]
                        (gdom/getElement "app"))))

(main)
