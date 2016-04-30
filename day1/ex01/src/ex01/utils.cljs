(ns ex01.utils
  (:require
   [ex01.state :as state]
   [thi.ng.strf.core :as f]))

(defn timed
  "Executes function f and returns vector of [execution-time result-of-f]"
  [f] (let [t0 (f/now) res (f)] [(- (f/now) t0) res]))

(defn conj-max
  "Takes a vector, size limit and value x. Appends x to vector and
  ensures vector does not grow beyond limit."
  [vec limit x]
  (let [n (count vec)]
    (if (>= n limit)
      (conj (subvec vec (inc (- n limit))) x)
      (conj vec x))))

(defn run-with-timer
  "Executes f, measures and records execution time,
  returns vector of avg. exec time of whole time window and result of f."
  [f]
  (let [[t res] (timed f)
        _       (swap! state/app update :samples conj-max (:num-samples @state/app) t)
        samples (get @state/app :samples)]
    [(/ (reduce + samples) (count samples)) res]))
