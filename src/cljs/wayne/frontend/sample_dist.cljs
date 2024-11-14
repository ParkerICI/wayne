(ns wayne.frontend.sample-dist
  (:require [re-frame.core :as rf]
            [wayne.frontend.signup :as signup]
            [com.hyperphor.way.web-utils :as wu]
            [com.hyperphor.way.vega :as v]
            com.hyperphor.way.params
            [com.hyperphor.way.download :as download]
            [reagent.dom]
            [wayne.frontend.feature-select :as fui]
            [wayne.frontend.heatmap :as hm]
            ))

(defn spec
  [data]
  (prn :data data)
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:values data}
   :mark {:type "rect"}
   :encoding {:y {:field "Tumor_Diagnosis" :type :nominal}
              :x {:field "WHO_grade" :type :nominal}
              :color {:field "samples" :type :quantitative}
              }} ;TOSO
  )

(defn sample-matrix
  []
  [v/vega-view (spec @(rf/subscribe [:data :dist-matrix {:fake :it}])) []])
