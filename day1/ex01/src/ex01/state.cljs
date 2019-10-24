(ns ex01.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [thi.ng.strf.core :as f]
   [reagent.core :as r]))

(defonce app
  (r/atom
   {:size        256
    :samples     []
    :num-samples 30
    :avg         0
    :mode        :naive}))

(defn set-mode!
  [e]
  (let [id (-> e .-target .-value keyword)]
    (debug :set-mode id)
    (swap! app assoc :mode id :samples [] :reset true)))

(defn set-size!
  [e]
  (let [size (-> e .-target .-value (f/parse-int 10))]
    (debug :set-size size)
    (swap! app assoc :size size :samples [] :reset true)))
