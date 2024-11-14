(ns wayne.frontend.sample-dist
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.vega :as v]
            ))

(defn spec
  [data]
  (prn :data data)
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:values data}
   :mark {:type "rect"}
   :encoding {:y {:field "Tumor_Diagnosis" :type :nominal}
              :x {:field "value" :type :nominal :title false}
              :facet {:field "dim" :type :nominal :title false}
              :color {:field "samples" :type :quantitative}
              }
   :resolve {:scale {:x "independent"}}}
  )

(defn sample-matrix
  []
  [v/vega-view (spec @(rf/subscribe [:data :dist-matrix {:fake :it}])) []])
