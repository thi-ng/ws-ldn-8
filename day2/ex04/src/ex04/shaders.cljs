(ns ex04.shaders
  (:require
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.gl.shaders.phong :as phong]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vertex]
   [thi.ng.glsl.lighting :as light]
   [thi.ng.glsl.fog :as fog]))

(glsl/defglsl tunnel-vs
  [vertex/surface-normal]
  "void main() {
     vUV = uv;
     vPos = (view * model * vec4(position, 1.0)).xyz;
     vNormal = surfaceNormal(normal, normalMat);
     vLightDir = (view * vec4(lightPos, 1.0)).xyz - vPos;
     gl_Position = proj * vec4(vPos, 1.0);
   }")

(glsl/defglsl tunnel-fs
  [fog/fog-linear light/beckmann-specular]
  "void main() {
     vec3 n = normalize(vNormal);
     vec3 v = normalize(-vPos);
     vec3 l = normalize(vLightDir);
     float NdotL = max(0.0, dot(n, l));
     vec3 specular = Ks * beckmannSpecular(l, v, n, m);
     vec3 att = lightCol / pow(length(vLightDir), lightAtt);
     vec3 diff = texture2D(tex, vUV).xyz;
     vec3 col = att * NdotL * ((1.0 - s) * diff + s * specular) + Ka * diff;
     float fog = fogLinear(length(vPos), fogDist.x, fogDist.y);
     col = mix(col, Kf, fog);
     gl_FragColor = vec4(col, 1.0);
   }")

(def tunnel-shader
  {:vs       (glsl/assemble tunnel-vs)
   :fs       (glsl/assemble tunnel-fs)
   :uniforms {:model     [:mat4 M44]
              :view      :mat4
              :proj      :mat4
              :normalMat :mat4
              :tex       :sampler2D
              :Ks        [:vec3 [1 1 1]]
              :Ka        [:vec3 [0.0 0.0 0.3]]
              :Kf        [:vec3 [0.0 0.0 0.1]]
              :m         [:float 0.5]
              :s         [:float 0.5]
              :lightCol  [:vec3 [1 1 1]]
              :lightPos  [:vec3 [0 0 5]]
              :lightAtt  [:float 3.0]
              :fogDist   [:vec2 [1 4.5]]
              :time      :float}
   :attribs  {:position :vec3
              :normal   :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2
              :vPos     :vec3
              :vNormal  :vec3
              :vLightDir :vec3}
   :state    {:depth-test true}})

(def player-shader phong/shader-spec)
