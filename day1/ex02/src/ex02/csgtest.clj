(with-open [o (io/output-stream "foo.stl")]
  (mio/write-stl
   (mio/wrapped-output-stream o)
   (g/tessellate (g/as-mesh (a/aabb 1)))))


;; [thi.ng.geom.mesh.csg]
;; [thi.ng.geom.mesh.io]
;; [thi.ng.geom.mesh.subdivision]

(let [a      (g/as-mesh (g/translate (s/sphere 1) [0.75 0 0]))
      b      (g/as-mesh (s/sphere 1))
      result (csg/csg->mesh
              (csg/substract
               (csg/mesh->csg b)
               (csg/mesh->csg a)))]
  (with-open [o (io/output-stream "foo.obj")]
    (mio/write-obj
     (mio/wrapped-output-stream o)
     result)))
