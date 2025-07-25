(ns org.parkerici.wayne.frontend.vitessce
  (:require [re-frame.core :as rf]
            [org.parkerici.wayne.frontend.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            com.hyperphor.way.feeds
            [reagent.dom]
            [reagent.core :as reagent]
            ["ag-grid-community" :as agx]
            [org.candelbio.multitool.core :as u]
            )
  )

(def col-defs
  [:sampleID :sample :panel :FOV])

;;; NOTE: for this to work, you need ./externs/app.txt containing at least withParams
(defn ag-grid-theme
  [base params]
  (.withParams base (clj->js params)))

;;; → way, somehow
(defmethod ag/ag-col-def :FOV 
  [col {:keys [url-template label-template] :as col-def}]
  {:headerName "FOV (Vitessce)"
   :field col
   :cellRenderer (fn [params]
                   (let [values (js->clj (.-data params) :keywordize-keys true)]
                     (reagent.core/as-element 
                      [:span.ag-cell-wrap-text   ;; .ag-cell-auto-height doesn't work, unfortunately.
                        [:a.ent
                         {:href (u/expand-template url-template values) :target "_ext"}
                         (u/expand-template label-template values)]])))
   }
  )

(defn table
  []
  [:div
   (let [fovs @(rf/subscribe [:data :vitessce {:fake :it}])] ;TODO param because there's bug in data feed mech
     [ag/ag-table 
      fovs
      :columns col-defs
      :class "sample-table"
      :autosize? true
      :col-defs {:FOV {:url-template "{{vitessce_link}}"
                       :label-template "{{FOV}}"
                       }}
      :ag-grid-options {:theme (ag-grid-theme agx/themeQuartz
                                              {:headerBackgroundColor "#020000",
                                               :headerFontSize 14,
                                               :headerFontWeight 600,
                                               :headerTextColor "#FFFFFF"
                                               :foregroundColor "black"
                                               :accentColor "#4586FF"
                                               })
                        :sideBar nil
                        :statusBar true
                        :pagination true
                        }
      ])])

(defn ^:export init
  []
  (init/init table nil)
  )

