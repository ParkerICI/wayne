(ns wayne.frontend.access
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            [org.candelbio.multitool.core :as u]
            [com.hyperphor.way.modal :as modal]
            [wayne.frontend.signup :as signup]
            [reagent.dom]
            ["ag-grid-community" :as agx]
            )
  )

;;; Was in raw-data-access.js, moved here for signup

(def data
  [

   ;; TEMP removed due to possible HIPAA issues?
   #_
   {
    :Description "Master Feature Table",
    :File "20240810_master_feature_table_na_removed_metadata.rds",
    :Size "35M",
    :Format "rds"
    },
   {
    :Description "Cell Table (immune)",
    :File "cell_table_immune_thresholded.parquet",
    :Size "1G",
    :Format "parquet"
    },
   {
    :Description "Cell Table (tumor)",
    :File "cell_table_tumor_thresholded.parquet",
    :Size "1G",
    :Format "parquet"
    }
   ]
  )

(def cols
  [:Description :File :Size :Format :download])

(defmethod ag/ag-col-def :download 
  [col _]
  (let [registered? @(rf/subscribe [:registered?])] ;Note: this needs to be outside the renderer
    {:headerName "Download"
     :field col
     :cellRenderer
     (fn [params]
       (let [item (js->clj (.-data params) :keywordize-keys true)]
         (reagent.dom/render ;TODO this is not approved for React 18, but I couldn't figure a better way.
           (if registered?
             [:span.ag-cell-wrap-text   ;; .ag-cell-auto-height doesn't work, unfortunately.
              [:a
               {:href (u/expand-template
                       "https://storage.googleapis.com/pici-bruce-vitessce-public/other/{{File}}"
                       item)
                :download (:File item)}
               [:img {:src "../assets/icons/download-dark.svg"}]]]
             [signup/signup-button])
           (.-eGridCell params)))
       )
     }
    ))

;;; NOTE: for this to work, you need ./externs/app.txt containing at least withParams
(defn ag-grid-theme
  [base params]
  (.withParams base (clj->js params)))

(defn ui
  []
  [:div
   [modal/modal]
   [ag/ag-table 
    data
    :columns cols
    :class "data-table"
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
    ]])

(defn ^:export init
  []
  (init/init ui nil)
  )

