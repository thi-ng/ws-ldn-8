(ns ex03.webgl07
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.utils :as glu]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.sphere :as s]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.typedarrays.core :as arrays]
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


(defn wrap-sphere [res]
  (attr/face-attribs
   (let [stride (/ 1.0 res)]
     (cycle
      (mapcat (fn [x]
                (map (fn [y]
                       (let [x (* x stride)
                             y (* y stride)]
                         [(vec2 x y) (vec2 (+ x stride) y)
                          (vec2 (+ x stride) (+ y stride)) (vec2 x (+ y stride))]))
                     (range 0 res)))
              (range 0 res))))))

(defn supplied-attrib
  "Attribute generator fn for types which emit pre-computed values
  as part of their `as-mesh` impl. Takes attrib key and for each vertex
  looks up value in opts map (which is supplied by supporting types,
  e.g. sphere)"
  ([attrib] (fn [_ id _ opts] (-> opts (get attrib) (nth id))))
  ([attrib cast] (fn [_ id _ opts] (-> opts (get attrib) (nth id) cast))))

(defn ^:export demo
  []
  (let [gl         (gl/gl-context "main")
        view-rect  (gl/get-viewport-rect gl)
        sphere-res 40
        model      (-> (s/sphere 1)
                       (g/center)
                       (g/as-mesh {:mesh    (glm/gl-mesh 4096 #{:uv})
                                   :res     sphere-res
                                   :attribs {:uv (supplied-attrib :uv vec2)}})
                       (gl/as-gl-buffer-spec {})
                       (cam/apply
                        (cam/perspective-camera
                         {:eye    (vec3 0 0 1.75)
                          :fov    90
                          :aspect view-rect}))
                       (assoc :shader (sh/make-shader-from-spec gl shader-spec))
                       (gl/make-buffers-in-spec gl glc/static-draw))
        tex-ready (volatile! false)
        ;; http://fossies.org/linux/misc/celestia-1.6.1.tar.gz/celestia-1.6.1/textures/hires/dione.jpg
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
           (gl/clear-color-and-depth-buffer 0 0 0 0 1)
           (gl/draw-with-shader
            (assoc-in model [:uniforms :model]
                      (-> M44 (g/rotate-x (m/radians 24.5)) (g/rotate-y (/ t 3)))))))
       true))))
