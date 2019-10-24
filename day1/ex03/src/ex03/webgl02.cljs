(ns ex03.webgl02
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.shaders.basic :as basic]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.color.core :as col]))

(defn ^:export demo
  [canvas]
  (let [gl        (gl/gl-context canvas)
        view-rect (gl/get-viewport-rect gl)
        model     (-> (a/aabb 1)
                      (g/center)
                      (g/as-mesh
                       {:mesh    (glm/indexed-gl-mesh 12 #{:col})
                        ;;:flags   :ewfb
                        :attribs {:col (->> [[1 0 0] [0 1 0] [0 0 1] [0 1 1] [1 0 1] [1 1 0]]
                                            (map col/rgba)
                                            (attr/const-face-attribs))}})
                      (gl/as-gl-buffer-spec {})
                      (cam/apply (cam/perspective-camera {:aspect view-rect}))
                      (assoc :shader (sh/make-shader-from-spec gl (basic/make-shader-spec-3d true)))
                      (gl/make-buffers-in-spec gl glc/static-draw))]
    (anim/animate
     (fn [t frame]
       (doto gl
         (gl/set-viewport view-rect)
         (gl/clear-color-and-depth-buffer col/WHITE 1)
         (gl/enable glc/depth-test)
         (gl/draw-with-shader
          (assoc-in model [:uniforms :model] (-> M44 (g/rotate-x t) (g/rotate-y (* t 2))))))
       true))))
