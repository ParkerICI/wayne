(ns wayne.frontend.sample-dist
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.vega :as v]
            ))

(defn spec
  [data]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:values data}
   :mark {:type "rect" :tooltip true}
   :encoding {:y {:field "Tumor_Diagnosis" :type :nominal}
              :x {:field "value" :type :nominal :title false}
              :facet {:field "dim" :type :nominal :title false :columns 5}
              :color {:field "samples" :type :quantitative
                      :legend {:orient :none :legendX -120 :legendY -75
                               :direction :horizontal
                               :gradientLength 300}}
              }
   :resolve {:scale {:x "independent"}}}
  )

(defn sample-matrix
  []
  [v/vega-view (spec @(rf/subscribe [:data :dist-matrix {:fake :it}])) []])
