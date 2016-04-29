(ns ex03.webgl04
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.color.core :as col]))

(enable-console-print!)

(def shader-spec
  {:vs "void main() {
    vUV = uv;
    gl_Position = proj * view * model * vec4(position, 1.0);
    }"
   :fs "void main() {
    gl_FragColor = texture2D(tex, vUV);
    }"
   :uniforms {:model    [:mat4 M44]
              :view     :mat4
              :proj     :mat4
              :tex      :sampler2D}
   :attribs  {:position :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2}
   :state    {:depth-test false
              :blend      true
              :blend-fn   [glc/src-alpha glc/one]}})

(defn ^:export demo
  []
  (let [gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        model     (-> (a/aabb 1)
                      (g/center)
                      (g/as-mesh
                       {:mesh    (glm/indexed-gl-mesh 12 #{:uv})
                        ;;:flags   :nsb
                        :attribs {:uv (attr/face-attribs (attr/uv-cube-map-v 256 false))}})
                      (gl/as-gl-buffer-spec {})
                      (assoc :shader (sh/make-shader-from-spec gl shader-spec))
                      (gl/make-buffers-in-spec gl glc/static-draw))
        tex-ready (volatile! false)
        tex       (buf/load-texture
                   gl {:callback (fn [tex img] (vreset! tex-ready true))
                       :src      "img/cubev.png"
                       :flip     true})]
    (anim/animate
     (fn [t frame]
       (when @tex-ready
         (gl/bind tex 0)
         (doto gl
           (gl/set-viewport view-rect)
           (gl/clear-color-and-depth-buffer 0.1 0.1 0.1 1 1)
           (gl/draw-with-shader
            (-> model
                (cam/apply
                 (cam/perspective-camera
                  {:eye (vec3 0 0 1.25)
                   ;;:up (m/normalize (vec3 (Math/sin t) 1 0))
                   :fov 90
                   :aspect view-rect}))
                (assoc-in [:uniforms :model]
                          (-> M44
                              (g/rotate-x (* HALF_PI (Math/sin t)))
                              (g/rotate-y (* t 2))))))))
       true))))
