(ns wayne.frontend.sites
  (:require [re-frame.core :as rf]
            [way.aggrid :as ag]
            [way.tabs :as tab]
            way.data
            [reagent.dom]
            )
  )

(defn sites
  []
  [:div
   [:h3 "Sites"]
   (let [sites @(rf/subscribe [:data :sites])]
     [ag/ag-table 
      :sites
      (keys (first sites))
      sites
      {}
      ])])

(defmethod tab/set-tab [:tab :sites]
  [db]
  (rf/dispatch [:fetch-once :sites]))
