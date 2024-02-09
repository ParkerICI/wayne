(ns wayne.frontend.patients
  (:require [re-frame.core :as rf]
            [way.aggrid :as ag]
            [way.tabs :as tab]
            way.data
            [reagent.dom]
            )
  )


(defn patients
  []
  [:div
   [:h3 "Patients"]
   (let [patients @(rf/subscribe [:data :patients])]
     [ag/ag-table 
      :patients
      (keys (first patients))
      patients
      {}
      ])])

(defmethod tab/set-tab [:tab :patients]
  [db]
  (rf/dispatch [:fetch-once :patients]))
