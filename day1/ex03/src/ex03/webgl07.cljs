(ns ex03.webgl07
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
   [thi.ng.geom.sphere :as s]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.color.core :as col]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vertex]))

(enable-console-print!)

(def shader-spec
  {:vs "void main() {
    vUV = uv;
    gl_Position = proj * view * model * vec4(position, 1.0);
    }"
   :fs "void main() { gl_FragColor = texture2D(tex, vUV); }"
   :uniforms {:model    [:mat4 M44]
              :view     :mat4
              :proj     :mat4
              :displace :sampler2D
              :tex      :sampler2D}
   :attribs  {:position :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2}
   :state    {:depth-test true}})

(defn ^:export demo
  []
  (let [gl         (gl/gl-context "main")
        view-rect  (gl/get-viewport-rect gl)
        sphere-res 40
        model      (-> (s/sphere 1)
                       (g/center)
                       (g/as-mesh {:mesh    (glm/gl-mesh 4096 #{:uv})
                                   :res     sphere-res
                                   :attribs {:uv (attr/supplied-attrib :uv vec2)}})
                       (gl/as-gl-buffer-spec {})
                       (cam/apply
                        (cam/perspective-camera
                         {:eye    (vec3 0 0 1.5)
                          :fov    90
                          :aspect view-rect}))
                       (assoc :shader (sh/make-shader-from-spec gl shader-spec))
                       (gl/make-buffers-in-spec gl glc/static-draw))
        tex-ready (volatile! false)
        tex       (buf/load-texture
                   gl {:callback (fn [tex img] (.generateMipmap gl (:target tex)) (vreset! tex-ready true))
                       :src      "img/earth.jpg"
                       :filter   [glc/linear-mipmap-linear glc/linear]
                       :flip     false})]
    (anim/animate
     (fn [t frame]
       (when @tex-ready
         (gl/bind tex 0)
         (doto gl
           (gl/set-viewport view-rect)
           (gl/clear-color-and-depth-buffer 0 0 0.05 1 1)
           (gl/draw-with-shader
            (assoc-in model [:uniforms :model]
                      (-> M44 (g/rotate-x (m/radians 24.5)) (g/rotate-y (/ t 3)))))))
       true))))
