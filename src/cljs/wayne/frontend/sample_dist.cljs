(ns wayne.frontend.sample-dist
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.vega :as v]
            [com.hyperphor.way.ui.init :as init]
            [wayne.frontend.utils :as wwu]
            ))

(defn spec
  [data]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:values data}
   :mark {:type "rect" :tooltip true}
   :encoding {:y {:field "Tumor_Diagnosis"
                  :type :nominal
                  :axis {:labelExpr "replace(datum.label, /_/g, ' ')"}
                  :title "Tumor Diagnosis"}
              :x {:field "value"
                  :type :nominal
                  :axis {:labelExpr "replace(datum.label, /_/g, ' ')"}
                  :title false}
              :facet {:field "dim"
                      :type :nominal
                      :title false
                      :header {:labelExpr "replace(datum.label, /_/g, ' ')"}
                      :columns 5}
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

(defn sample-matrix-with-popout
  []
  [:div
   [:div {:style {:float :right}}
    [wwu/popout-button "/sm-popout" :width 700 :height 800 :id "samplematrix"]]
   [sample-matrix]
   ])

(defn ^:export sample-matrix-popout
  []
  (init/init sample-matrix nil))

