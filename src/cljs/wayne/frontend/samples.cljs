(ns wayne.frontend.samples
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            com.hyperphor.way.feeds
            [reagent.dom]
            [wayne.frontend.data :as data]
            )
  )

(defn samples
  []
  [:div
   [:h3 "Samples!"]
   (let [samples @(rf/subscribe [:data :samples {:fake :it}])] ;TODO param because there's bug in data feed mech
     [ag/ag-table 
      samples
      :col-defs data/col-defs
      ])])

(defn ^:export init
  []
  (init/init samples nil)
  )

