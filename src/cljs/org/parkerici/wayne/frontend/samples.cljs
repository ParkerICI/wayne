(ns org.parkerici.wayne.frontend.samples
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            com.hyperphor.way.feeds
            [reagent.dom]
            ["ag-grid-community" :as agx]
            )
  )

;; Somewhat misnamed, this is a patients table with sample ids in a list columns

(def col-defs
  [:patient_id :samples, :who_grade, :diagnosis, :immunotherapy, :site])

;;; NOTE: for this to work, you need ./externs/app.txt containing at least withParams
(defn ag-grid-theme
  [base params]
  (.withParams base (clj->js params)))

;;; TODO col-defs causing console errors, need to fix that.
(defn samples
  []
  [:div
   (let [samples @(rf/subscribe [:data :patients {:fake :it}])] ;TODO param because there's bug in data feed mech
     [ag/ag-table 
      samples
      :columns col-defs
      :class "sample-table"
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
  (init/init samples nil)
  )

