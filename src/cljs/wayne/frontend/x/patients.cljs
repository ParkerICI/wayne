(ns wayne.frontend.patients
  (:require [re-frame.core :as rf]
            [wayne.frontend.data :as data]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.tabs :as tab]
            [reagent.dom]
            )
  )

(defn patients
  []
  [:div
   [:h3 "Patients"]
   (let [patients @(rf/subscribe [:data :patients])] ;OBSO?
     [ag/ag-table 
      :patients
      (keys (first patients))
      patients
      {}
      :col-defs data/col-defs
      ])])

(defmethod tab/set-tab [:tab :patients]
  [id tab db]
  (rf/dispatch [:fetch-once :patients]))
