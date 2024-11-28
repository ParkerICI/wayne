(ns wayne.frontend.vitessce
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            com.hyperphor.way.feeds
            [reagent.dom]
            ["ag-grid-community" :as agx]
            )
  )

(def col-defs
  [:sampleID :panel :FOV :vitessce_link])

;;; NOTE: for this to work, you need ./externs/app.txt containing at least withParams
(defn ag-grid-theme
  [base params]
  (.withParams base (clj->js params)))

;;; TODO col-defs causing console errors, need to fix that.
(defn table
  []
  [:div
   (let [fovs @(rf/subscribe [:data :vitessce {:fake :it}])] ;TODO param because there's bug in data feed mech
     [ag/ag-table 
      fovs
      :columns col-defs
      :class "sample-table"
      :col-defs {:vitessce_link {:url-template "%s" }}
      :ag-grid-options {:theme (ag-grid-theme agx/themeQuartz
                                              {:headerBackgroundColor "#020000",
                                               :headerFontSize 14,
                                               :headerFontWeight 600,
                                               :headerTextColor "#FFFFFF"
                                               :foregroundColor "black"
                                               :accentColor "#4586FF"
                                               })
                        :sideBar nil
                        :statusBar nil
                        }
      ])])

(defn ^:export init
  []
  (init/init table nil)
  )

