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
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.utils :as gu]
   [thi.ng.color.core :as col]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(defonce app
  (reagent/atom
   {:stream {:state :wait}
    :curr-shader :thresh}))

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
              :width  (.-width video)
              :height (.-height video)
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
  (let [vw        640
        vh        480
        gl        (gl/gl-context (reagent/dom-node this))
        view-rect (gl/get-viewport-rect gl)
        thresh    (sh/make-shader-from-spec gl shaders/threshold-shader-spec)
        hue-shift (sh/make-shader-from-spec gl shaders/hueshift-shader-spec)
        twirl     (sh/make-shader-from-spec gl shaders/twirl-shader-spec)
        pixelate  (sh/make-shader-from-spec gl shaders/pixelate-shader-spec)
        tile      (sh/make-shader-from-spec gl shaders/tile-shader-spec)
        fbo-tex   (buf/make-texture
                   gl {:width  512
                       :height 512
                       :filter glc/linear
                       :wrap   glc/clamp-to-edge})
        fbo       (buf/make-fbo-with-attachments
                   gl {:tex    fbo-tex
                       :width  512
                       :height 512
                       :depth? true})]
    (swap! app merge
           {:gl          gl
            :view        view-rect
            :shaders     {:thresh    thresh
                          :hue-shift hue-shift
                          :twirl     twirl
                          :tile      tile
                          :pixelate  pixelate}
            :scene       {:fbo     fbo
                          :fbo-tex fbo-tex
                          :cube    (-> (a/aabb 1)
                                       (g/center)
                                       (g/as-mesh
                                        {:mesh    (glm/indexed-gl-mesh 12 #{:uv})
                                         :attribs {:uv attr/uv-faces}})
                                       (gl/as-gl-buffer-spec {})
                                       (assoc :shader (sh/make-shader-from-spec gl shaders/cube-shader-spec))
                                       (gl/make-buffers-in-spec gl glc/static-draw))
                          :img     (-> (fx/init-fx-quad gl)
                                       #_(assoc :shader thresh))}})
    (init-rtc-stream vw vh)))

(defn update-app
  [this]
  (fn [t frame]
    (let [{:keys [gl view scene stream shaders curr-shader]} @app]
      (when-let [tex (get-in scene [:img :shader :state :tex])]
        (gl/configure tex {:image (:video stream)})
        (gl/bind tex)
        ;; render to texture
        ;; (gl/bind (:fbo scene))
        (doto gl
          ;;(gl/set-viewport 0 0 512 512)
          (gl/clear-color-and-depth-buffer col/BLACK 1)
          (gl/draw-with-shader
           (-> (:img scene)
               (assoc-in [:uniforms :time] t)
               (assoc :shader (shaders curr-shader)))))
        ;;(gl/unbind (:fbo scene))
        ;; render cube to main canvas
        ;;(gl/bind (:fbo-tex scene) 0)
        #_(doto gl
          (gl/set-viewport view)
          (gl/draw-with-shader
           (-> (:cube scene)
               (cam/apply
                (cam/perspective-camera
                 {:eye (vec3 0 0 1.25) :fov 90 :aspect view}))
               (assoc-in [:uniforms :model] (-> M44 (g/rotate-x t) (g/rotate-y (* t 2))))))))
      (:active (reagent/state this)))))
