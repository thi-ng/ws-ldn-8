(ns ex07.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [thi.ng.math.core :as m]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.domus.core :as dom]
   [thi.ng.color.core :as col]
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


(defn init-app-2d
  [this]
  (let [canvas      (reagent/dom-node this)
        ctx         (.getContext canvas "2d")
        psys        (.ccall js/Module "initParticleSystem" "*"
                            #js ["number" "number" "number" "number"]
                            #js [10000 1000 (/ (.-width canvas) 2) -0.1 3])
        psys-update (.cwrap js/Module "updateParticleSystem" "*" #js ["number"])
        psys-count  (.cwrap js/Module "getNumParticles" "number" #js ["number"])
        psys-get    (.cwrap js/Module "getParticleComponent" "number" #js ["number" "number" "number"])]
    (swap! app merge
           {:psys        psys
            :psys-update psys-update
            :psys-count  psys-count
            :psys-get    psys-get
            :canvas      canvas
            :ctx         ctx})))

(defn update-app-2d
  [this]
  (fn [t frame]
    (let [{:keys [canvas ctx psys psys-update psys-count psys-get]} @app
          _   (psys-update #js [psys])
          num (psys-count #js [psys])]
      (set! (.-width canvas) (.-width canvas))
      (set! (.-fillStyle ctx) "red")
      (loop [i 0]
        (when (< i num)
          (let [x (psys-get psys i 0)
                y (psys-get psys i 1)]
            (doto ctx
              (.beginPath)
              (.arc x y 2 0 m/TWO_PI)
              (.fill))
            (recur (inc i)))))
      (:active (reagent/state this)))))

(def shader-spec
  {:vs "void main() {
    vCol = color;
    gl_Position = proj * view * model * vec4(position, 1.0);
    gl_PointSize = 4.0;
    }"
   :fs "void main() {
    gl_FragColor = vec4(vCol, 1.0);
    }"
   :uniforms {:model    [:mat4 M44]
              :view     :mat4
              :proj     :mat4}
   :attribs  {:position :vec3
              :color    :vec3}
   :varying  {:vCol :vec3}
   :state    {:depth-test false
              :blend      true
              :blend-fn   [glc/src-alpha glc/one]}})

(defn attrib-buffer-view
  [ptr stride num]
  (js/Float32Array. (.-buffer (aget js/Module "HEAPU8")) ptr (* stride num)))

(defn init-app-3d
  [this]
  (let [psys         (.ccall js/Module "initParticleSystem" "*"
                             #js ["number" "number" "number" "number"]
                             #js [10000 1000 0.0 -0.01 0.125])
        psys-update  (.cwrap js/Module "updateParticleSystem" "*" #js ["number"])
        psys-count   (.cwrap js/Module "getNumParticles" "number" #js ["number"])
        psys-get     (.cwrap js/Module "getParticleComponent" "number" #js ["number" "number" "number"])
        particle-ptr (.ccall js/Module "getParticlesPointer" "number" #js ["number"] #js [psys])
        gl           (gl/gl-context (reagent/dom-node this))
        particles    (-> {:attribs      {:position {:data   (attrib-buffer-view particle-ptr 9 10000)
                                                    :size   3
                                                    :stride 36}
                                         :color    {:data   (attrib-buffer-view (+ particle-ptr 24) 9 10000)
                                                    :size   3
                                                    :stride 36}}
                          :num-vertices 10000
                          :mode         glc/points}
                         (gl/make-buffers-in-spec gl glc/dynamic-draw)
                         (assoc :shader (sh/make-shader-from-spec gl shader-spec))
                         (update :uniforms merge
                                 {:view (mat/look-at (vec3 0 2 2) (vec3 0 1 0) (vec3 0 1 0))
                                  :proj (mat/perspective 90 (/ 16 9) 0.1 100)}))]
    (swap! app merge
           {:psys         psys
            :psys-update  psys-update
            :psys-count   psys-count
            :psys-get     psys-get
            :gl           gl
            :particle-ptr particle-ptr
            :scene        {:particles particles}})))

(defn update-app-3d
  [this]
  (fn [t frame]
    (let [{:keys [gl psys psys-update psys-count particle-ptr scene]} @app
          _   (psys-update #js [psys])
          num (psys-count #js [psys])]
      (.bindBuffer gl glc/array-buffer
                   (get-in scene [:particles :attribs :position :buffer]))
      (.bufferData gl glc/array-buffer
                   (attrib-buffer-view particle-ptr 9 10000)
                   glc/dynamic-draw)
      (.bindBuffer gl glc/array-buffer
                   (get-in scene [:particles :attribs :color :buffer]))
      (.bufferData gl glc/array-buffer
                   (attrib-buffer-view (+ particle-ptr 24) 9 10000)
                   glc/dynamic-draw)
      (doto gl
        (gl/clear-color-and-depth-buffer (col/rgba 0 0 0.1) 1)
        (gl/draw-with-shader
         (-> (:particles scene)
             (assoc :num-vertices num)
             (assoc-in [:uniforms :model] (-> M44 (g/rotate-y (* t 0.25)) (g/scale 0.1))))))
      (:active (reagent/state this)))))

(defn main
  []
  (.initializeTouchEvents js/React)
  (reagent/render-component
   [canvas-component {:init init-app-3d
                      :loop update-app-3d}]
   (dom/by-id "app")))

(main)
