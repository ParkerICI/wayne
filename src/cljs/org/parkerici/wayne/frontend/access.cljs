(ns org.parkerici.wayne.frontend.access
  (:require [re-frame.core :as rf]
            [org.parkerici.wayne.frontend.way.aggrid :as ag]
            [com.hyperphor.way.ui.init :as init]
            [org.candelbio.multitool.core :as u]
            [com.hyperphor.way.modal :as modal]
            [org.parkerici.wayne.frontend.signup :as signup]
            [reagent.dom]
            ["ag-grid-community" :as agx]
            )
  )

;;; Was in raw-data-access.js, moved here for signup
;;; Source: https://drive.google.com/drive/u/0/folders/1dfcMJYpDQ_PmvacR39z-dYHl-G8KpmeB
;;;         https://drive.google.com/drive/u/1/folders/10UmqEQriNVxd4QZM328vwMhVaS_vace4
;;; Served from: gs://pici-bruce-vitessce-public/other
;;;   gcloud storage cp ... gs://pici-bruce-vitessce-public/other
(def data
  [
   ;; removed due to possible HIPAA issues
   #_
   {
    :Description "Master Feature Table",
    :File "20240810_master_feature_table_na_removed_metadata.rds",
    :Size "35M"
    }

   ;; Not on GDrive so removing for now
   #_
   {
    :Description "Cell Table (immune)",
    :File "cell_table_immune_thresholded.parquet",
    :Size "1G"
    }
   #_
   {
    :Description "Cell Table (tumor)",
    :File "cell_table_tumor_thresholded.parquet",
    :Size "1G"
    }

   {:File "cell_table_all_merged_thresholded.parquet"
    :Size "2.0G"
    :Description "Cell Table (merged)"
    :disabled? true
    }
   {:File "20240702_Stanford_MALDI_annotated.csv"
    :Size "400K"
    }
   {:File "20241112_MALDI_BRUCE_annotated.csv"
    :Size "809K"
    }
   {:File "all_cell_count_tumor_FOV_result.parquet"
    :Size "7.2K"
    }
   {:File "auc_data_survival_status.csv"
    :Size "310"
    }
   {:File "auc_data_who_grade.csv"
    :Size "309"
    }

   {:File "gbm_mean_by_stage_table_norm_gly_enz_new.csv"
    :Size "3.8K"
    }
   {:File "glycan_cell_significant_data_fig_5.rds"
    :Size "658"
    }
   {:File "glycan_classes.csv"
    :Size "6.2K"
    }
   {:File "merged_all_top_go_df_fig_5.rds"
    :Size "2.2K"
    }
   {:File "metadata_complete.csv"
    :Size "330K"
    }
   {:File "pca_subset.csv"
    :Size "982"
    }
   {:File "rf_gini_importance_df_survival_status.csv"
    :Size "764K"
    }
   {:File "rf_gini_importance_df_who_grade.csv"
    :Size "749K"
    }
   {:File "tme_gene_signatures.csv"
    :Size "2.4K"
    }
   {:File "top_seven_df.parquet"
    :Size "37K"
    }
   {:File "tumor_count_result.parquet"
    :Size "7.1K"
    }
   {:File "NS_R_files.zip"
    :Size "83.9M"
    }
   {:File "20221006_GBM_NS_pipeline_cleaned.rds"
    :Size "15.3M"
    }
   ]
  )

(def cols
  ;; :Description removed until scientists decide to provide text
  ;; Or see https://www.perplexity.ai/search/please-write-short-description-LoiOlcsITSe2GI2oMJdTNQ
  [#_ :Description :File :Size :Format :download])

(defmethod ag/ag-col-def :Format
  [_ _]
  {:headerName "Format"
   :valueGetter (fn [row]
                  (let [file (.-File (.-data row))]
                    (second (re-find #"\.(.*)$" file))))})

;;; OK this is dumb, better to store the real number and humanize on output
(defn dehumanize
  [hnum]
  (let [[_ num mag] (re-matches #"([\d\.]+)(\D)?" hnum)
        num (u/coerce-numeric num)]
    (case mag
      nil num
      "K" (* num 1000)
      "M" (* num 1000000)
      "G" (* num 1000000000))))

(defn compare-humanized-nums
  [a b]
  (compare (dehumanize a) (dehumanize b)))

(defmethod ag/ag-col-def :Size
  [_ _]
  {:headerName "Size"
   :field :Size
   :comparator (fn [a b & _]
                 (compare-humanized-nums a b))})


(defmethod ag/ag-col-def :download 
  [col _]
  (let [registered? @(rf/subscribe [:registered?])] ;Note: this needs to be outside the renderer
    {:headerName "Download"
     :field col
     :cellRenderer
     (fn [params]
       (let [item (js->clj (.-data params) :keywordize-keys true)]
         (when-not (:disabled? item)
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
             (.-eGridCell params)))))
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
    :autosize? true
    :class "data-table"
    :ag-grid-options {:theme (ag-grid-theme agx/themeQuartz
                                            {:headerBackgroundColor "#020000",
                                             :headerFontSize 14,
                                             :headerFontWeight 600,
                                             :headerTextColor "#FFFFFF"
                                             :foregroundColor "black"
                                             :accentColor "#4586FF"
                                             })
                      :pagination false
                      :sideBar nil
                      :statusBar nil
                      }
    ]])

(defn ^:export init
  []
  (init/init ui nil)
  )

