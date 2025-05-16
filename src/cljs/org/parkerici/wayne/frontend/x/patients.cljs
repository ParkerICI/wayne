(ns org.parkerici.wayne.frontend.x.patients
  (:require [re-frame.core :as rf]
            [org.parkerici.wayne.frontend.data :as data]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.tabs :as tab]
            [reagent.dom]
            )
  )

(defn patients
  []
  [:div
   [:h3 "Patients"]
   (let [patients @(rf/subscribe [:data :patients])] 
     [ag/ag-table 
      patients
      ])])

(defn metadata-full
  []
  [:div
   [:h3 "Metadata"]
   (let [metadata @(rf/subscribe [:data :metadata {:fake :it}])] 
     [ag/ag-table 
      metadata
      ])])

(defmethod tab/set-tab [:tab :patients]
  [id tab db]
  (rf/dispatch [:fetch-once :patients]))
