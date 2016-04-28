(ns ex02.shapes01
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.polygon :as poly]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.domus.core :as dom]))

(enable-console-print!)

(defn ^:export main
  []
  (dom/create-dom!
   (svg/svg
    {:width 640 :height 480}
    (-> (poly/cog 0.5 20 [0.9 1 1 0.9 0.3])
        (g/scale 480)
        (g/translate [320 240])
        (g/vertices)
        (svg/polygon {:fill "black" :stroke "#0ff"})))
   (dom/by-id "app")))

;;(main)



