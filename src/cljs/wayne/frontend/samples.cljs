(ns wayne.frontend.samples
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            com.hyperphor.way.feeds
            [reagent.dom]
            )
  )

(def col-defs
  [:patient_id :sample_id, :who_grade, :final_diagnosis_simple, :immunotherapy, :site])

(defn samples
  []
  [:div
   (let [samples @(rf/subscribe [:data :samples {:fake :it}])] ;TODO param because there's bug in data feed mech
     [ag/ag-table 
      samples
      :columns col-defs
      :class "sample-table"
      ])])

(defn ^:export init
  []
  (init/init samples nil)
  )

