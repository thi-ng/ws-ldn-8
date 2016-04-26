(ns ex02.utils
  (:require
   [thi.ng.strf.core :as f]))

(defn timed
  "Executes function f and returns its execution time in ms."
  [f] (let [t0 (f/now)] (f) (- (f/now) t0)))

(defn conj-max
  "Takes a vector, size limit and value x. Appends x to vector and
  ensures vector does not grow beyond limit."
  [vec limit x]
  (let [n (count vec)]
    (if (>= n limit)
      (conj (subvec vec (inc (- n limit))) x)
      (conj vec x))))

(defn run-with-timer
  "Takes a volatile containing vector of timing samples, window size
  and function f. Executes f and measures and records execution time,
  returns avg. exec time of whole time window."
  [samples window f]
  (let [samples (vswap! samples conj-max window (timed f))]
    (/ (reduce + samples) (count samples))))
