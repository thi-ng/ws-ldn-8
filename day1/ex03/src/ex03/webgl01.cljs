(ns ex03.webgl01
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.polygon :as poly]))

(enable-console-print!)

(def shader-spec
  {:vs       "void main() { gl_Position = proj * view * model * vec4(position, 0.0, 1.0); }"
   :fs       "void main() { gl_FragColor = color; }"
   :uniforms {:proj  :mat4
              :model [:mat4 M44]
              :view  [:mat4 M44]
              :color [:vec4 [0 0 0 1]]}
   :attribs  {:position :vec2}
   :state    {:depth false}})

(defn ^:export demo
  []
  (let [teeth     20
        gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        model     (-> (poly/cog 0.5 teeth [0.9 1 1 0.9])
                      (gl/as-gl-buffer-spec {:normals false})
                      (gl/make-buffers-in-spec gl glc/static-draw)
                      (assoc-in [:uniforms :proj] (gl/ortho view-rect))
                      (assoc :shader (sh/make-shader-from-spec gl shader-spec)))]
    (anim/animate
     (fn [t frame]
       (doto gl
         (gl/set-viewport view-rect)
         (gl/clear-color-and-depth-buffer 0.9 0.9 0.92 1 1)
         ;; draw left polygon
         (gl/draw-with-shader
          (update model :uniforms merge
                  {:model (-> M44 (g/translate -0.48 0 0) (g/rotate t))
                   :color [0 1 1 1]}))
         ;; draw right polygon
         (gl/draw-with-shader
          (update model :uniforms merge
                  {:model (-> M44 (g/translate 0.48 0 0) (g/rotate (- (+ t (/ HALF_PI teeth)))))
                   :color [0 1 0 1]})))
       true))))
