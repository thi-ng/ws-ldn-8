(ns ex05.core
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [reagent.core :as r]))

(def app (r/atom {}))

(enable-console-print!)

(defn update-worker-state
  "Callback handler for worker messages."
  [msg]
  (prn :received (.-data msg))
  (swap! app assoc :worker-msg (.-data msg)))

(defn start-worker
  "Handler to send :start command to worker, enables
  :worker-active key in app state."
  []
  (.postMessage
   (:worker @app)
   (pr-str {:command :start :interval 1000}))
  (swap! app assoc :worker-msg nil :worker-active true))

(defn stop-worker
  "Handler to send :stop command to worker, disables :worker-active
  key in app state atom."
  []
  (.postMessage
   (:worker @app)
   (pr-str {:command :stop}))
  (swap! app assoc :worker-active false))

(defn app-component
  "Main reagent app component"
  []
  (let [msg     (reaction (:worker-msg @app))
        active? (reaction (:worker-active @app))]
    (fn []
      [:div
       [:h1 "Worker example"]
       (if @active?
         [:div
          [:p "Latest message from worker:"]
          [:p (if @msg
                [:textarea
                 {:cols 60
                  :rows 5
                  :value (pr-str @msg)}]
                "Waiting...")]
          [:p [:button {:on-click stop-worker} "Stop"]]]
         [:div
          [:p "Worker idle..."]
          [:button {:on-click start-worker} "Start"]])])))

(defn init-app
  "Initializes worker & stores handle in app state atom."
  []
  (let [worker (js/Worker. "js/worker.js")]
    (set! (.-onmessage worker) update-worker-state)
    (reset! app {:worker worker})))

(defn main
  []
  (init-app)
  (r/render-component [app-component] (.-body js/document)))

(main)
