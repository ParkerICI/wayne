(ns wayne.frontend.samples
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            com.hyperphor.way.feeds
            [reagent.dom]
            )
  )

;; Somewhat misnamed, this is a patients table with sample ids in a list columns

(def col-defs
  [:patient_id :samples, :who_grade, :diagnosis, :immunotherapy, :site])

(defn samples
  []
  [:div
   (let [samples @(rf/subscribe [:data :patients {:fake :it}])] ;TODO param because there's bug in data feed mech
     [ag/ag-table 
      samples
      :columns col-defs
      :class "sample-table"
      ])])

(defn ^:export init
  []
  (init/init samples nil)
  )

