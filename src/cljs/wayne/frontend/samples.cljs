(ns wayne.frontend.samples
  (:require [re-frame.core :as rf]
            [hyperphor.way.aggrid :as ag]
            [hyperphor.way.tabs :as tab]
            [reagent.dom]
            [wayne.frontend.data :as data]
            )
  )

(defn samples
  []
  [:div
   [:h3 "Samples"]
   (let [samples @(rf/subscribe [:data :samples])]
     [ag/ag-table 
      samples
      :col-defs data/col-defs
      ])])

(defmethod tab/set-tab [:tab :samples]
  [id tab db]
  (rf/dispatch [:fetch-once :samples]))
