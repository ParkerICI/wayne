(ns wayne.frontend.samples
  (:require [re-frame.core :as rf]
            [way.aggrid :as ag]
            [way.tabs :as tab]
            way.data
            [reagent.dom]
            )
  )

(defn samples
  []
  [:div
   [:h3 "Samples"]
   (let [samples @(rf/subscribe [:data :samples])]
     [ag/ag-table 
      :samples
      (keys (first samples))
      samples
      {}
      ])])

(defmethod tab/set-tab [:tab :samples]
  [db]
  (rf/dispatch [:fetch-once :samples]))
