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
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.domus.core :as dom]
   [thi.ng.color.core :as col]
   [reagent.core :as reagent]))

(defonce app (reagent/atom {:mpos [0 0]}))

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
        psys        (.ccall js/Particles "initParticleSystem" "*"
                            #js ["number" "number" "number" "number"]
                            #js [10000 1000 (/ (.-width canvas) 2) -0.1 3])
        psys-update (.cwrap js/Particles "updateParticleSystem" "*" #js ["number"])
        psys-count  (.cwrap js/Particles "getNumParticles" "number" #js ["number"])
        psys-get    (.cwrap js/Particles "getParticleComponent" "number" #js ["number" "number" "number"])]
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
          _   (psys-update psys)
          num (psys-count psys)]
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
    vCol = vec4(color, 1.0);
    gl_Position = proj * view * model * vec4(position, 1.0);
    gl_PointSize = 16.0 - gl_Position.w * 1.5;
    }"
   :fs "void main() {
    gl_FragColor = texture2D(tex, gl_PointCoord) * vCol;
    }"
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :tex        [:sampler2D 0]}
   :attribs  {:position   :vec3
              :color      :vec3}
   :varying  {:vCol       :vec4}
   :state    {:depth-test false
              :blend      true
              :blend-fn   [glc/src-alpha glc/one]}})

(defn attrib-buffer-view
  [ptr stride num]
  (js/Float32Array. (.-buffer (aget js/Particles "HEAPU8")) ptr (* stride num)))

(defn update-attrib-buffer
  [gl attrib ptr stride num]
  (.bindBuffer gl glc/array-buffer
               (get-in (:scene @app) [:particles :attribs attrib :buffer]))
  (.bufferData gl glc/array-buffer
               (attrib-buffer-view ptr stride num)
               glc/dynamic-draw))

(defn init-app-3d
  [this]
  (let [psys         (.ccall js/Particles "initParticleSystem" "*"
                             #js ["number" "number" "number" "number"]
                             #js [10000 1000 0.0 -0.01 0.125])
        psys-update  (.cwrap js/Particles "updateParticleSystem" "*" #js ["number"])
        psys-count   (.cwrap js/Particles "getNumParticles" "number" #js ["number"])
        psys-get     (.cwrap js/Particles "getParticleComponent" "number" #js ["number" "number" "number"])
        particle-ptr (.ccall js/Particles "getParticlesPointer" "number" #js ["number"] #js [psys])
        gl           (gl/gl-context (reagent/dom-node this))
        view         (gl/get-viewport-rect gl)
        tex          (buf/load-texture
                      gl {:callback (fn [tex img] (swap! app assoc :tex-ready true))
                          :src      "img/tex32.png"})
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
                         (assoc-in [:shader :state :tex] tex)
                         (update :uniforms merge
                                 {:view (mat/look-at (vec3 0 2 2) (vec3 0 1 0) (vec3 0 1 0))
                                  :proj (mat/perspective 90 view 0.1 100)}))]
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
    (when (:tex-ready @app)
      (let [{:keys [gl psys psys-update psys-count particle-ptr scene mpos]} @app
            _   (psys-update psys)
            num (psys-count psys)]
        (update-attrib-buffer gl :position particle-ptr 9 10000)
        (update-attrib-buffer gl :color (+ particle-ptr 24) 9 10000)
        (doto gl
          (gl/clear-color-and-depth-buffer (col/rgba 0 0 0.1) 1)
          (gl/draw-with-shader
           (-> (:particles scene)
               (assoc :num-vertices num)
               (assoc-in [:uniforms :model]
                         (-> M44
                             (g/rotate-x (* (mpos 1) 0.001))
                             (g/rotate-y (* (mpos 0) 0.001))
                             (g/scale 0.1))))))))
    (:active (reagent/state this))))

(defn main
  []
  ;; first initialize C module
  (js/Particles)
  (reagent/render-component
   [canvas-component
    {:init          init-app-3d
     :loop          update-app-3d
     :on-mouse-move #(swap! app assoc :mpos [(.-clientX %) (.-clientY %)])}]
   (dom/by-id "app")))

(main)
