(ns ex06.main
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ex06.shaders :as shaders]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.utils :as glu]
   [thi.ng.geom.gl.fx :as fx]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.rect :as r]
   [thi.ng.geom.utils :as gu]
   [thi.ng.color.core :as col]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(defonce app
  (reagent/atom
   {:stream {:state :wait}}))

(defn set-stream-state!
  [state] (swap! app assoc-in [:stream :state] state))

(defn set-shader!
  [id] (swap! app assoc :curr-shader (keyword id)))

(defn init-video-texture
  [video]
  (let [tex (buf/make-canvas-texture
             (:gl @app) video
             {:filter glc/linear
              :wrap   glc/clamp-to-edge
              :width  640
              :height 480
              :flip   true
              :premultiply false})]
    (swap! app assoc-in [:scene :img :shader :state :tex] tex)))

(defn activate-rtc-stream
  [video stream]
  (swap! app assoc-in [:stream :video] video)
  (set! (.-onerror video)
        (fn [] (.stop stream) (set-stream-state! :error)))
  (set! (.-onended stream)
        (fn [] (.stop stream) (set-stream-state! :stopped)))
  (set! (.-src video)
        (.createObjectURL (or (aget js/window "URL") (aget js/window "webkitURL")) stream))
  (set-stream-state! :ready)
  (init-video-texture video))

(defn init-rtc-stream
  [w h]
  (let [video (dom/create-dom!
               [:video {:width w :height h :hidden true :autoplay true}]
               (.-body js/document))]
    (cond
      (aget js/navigator "webkitGetUserMedia")
      (.webkitGetUserMedia js/navigator #js {:video true}
                           #(activate-rtc-stream video %)
                           #(set-stream-state! :forbidden))
      (aget js/navigator "mozGetUserMedia")
      (.mozGetUserMedia js/navigator #js {:video true}
                        #(activate-rtc-stream video %)
                        #(set-stream-state! :forbidden))
      :else
      (set-stream-state! :unavailable))))

(defn init-app
  [this]
  (let [gl        (gl/gl-context (reagent/dom-node this))
        view-rect (gl/get-viewport-rect gl)
        thresh    (sh/make-shader-from-spec gl shaders/threshold-shader-spec)
        hue-shift (sh/make-shader-from-spec gl shaders/hueshift-shader-spec)
        twirl     (sh/make-shader-from-spec gl shaders/twirl-shader-spec)]
    (swap! app merge
           {:gl          gl
            :view        view-rect
            :curr-shader :thresh
            :shaders     {:thresh    thresh
                          :hue-shift hue-shift
                          :twirl     twirl}
            :scene       {:img (-> (fx/init-fx-quad gl)
                                   (assoc :shader thresh))}})
    (init-rtc-stream 640 480)))

(defn update-app
  [this]
  (fn [t frame]
    (let [{:keys [gl view scene stream shaders curr-shader]} @app]
      (when-let [tex (get-in scene [:img :shader :state :tex])]
        (gl/configure tex {:image (:video stream)})
        (gl/bind tex)
        (doto gl
          (gl/set-viewport view)
          (gl/draw-with-shader
           (-> (:img scene)
               (assoc-in [:uniforms :time] t)
               (assoc :shader (shaders curr-shader))))))
      (:active (reagent/state this)))))
