(ns ex01.core
  (:require
   [ex01.naive :as naive]
   [ex01.faster :as faster]
   [ex01.fastest :as fastest]
   [thi.ng.domus.core :as dom]))

(enable-console-print!)

(def width 256)
(def height 256)

(defn main
  []
  (let [canvas (dom/create-dom!
                [:canvas {:width width :height height}]
                (dom/by-id "app"))
        ctx    (.getContext canvas "2d")]
    (naive/main canvas ctx width height)     ;; 594 ms (advanced compile, else x2)
    ;;(faster/main canvas ctx width height)  ;; 84ms
    ;;(fastest/main canvas ctx width height) ;; 2.3ms
    ))

(main)

