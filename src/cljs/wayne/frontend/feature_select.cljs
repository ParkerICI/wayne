(ns wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [wayne.frontend.data :as data]
            [way.web-utils :as wu]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            )
  )

;;; Complex enough to get its own file

;;; could drive these from a query and API
(def cell-meta-clusters
  ["APC"
   "Bcells"
   "DC_CD123"
   "DC_CD206"
   "DC_Mac_CD209"
   "Endothelial_cells"
   "Immune_unassigned"
   "Macrophage_CD163"
   "Macrophage_CD206"
   "Macrophage_CD68"
   "Mast_cells"
   "Microglia"
   "Myeloid_CD11b"
   "Myeloid_CD14"
   "Myeloid_CD141"
;;; I think this option should not appear?
;   "NA"
   "Neurons"
   "Neutrophils"
   "Tcell_CD4"
   "Tcell_CD8"
   "Tcell_FoxP3"
   "Tumor_cells"
   "Unassigned"])

;;; Defines chooses for level 2 and 3
(def non-spatial-features-2-3
  [["marker_intensity" []]                ;special cased
   ;; orange
   ["tumor_cell_features" ["tumor_antigen_co_relative"
                           "tumor_antigen_fractions"
                           "tumor_antigen_spatial_density"]]
   ;; purple
   ["immune_tumor_cell_features" ["immune_cell_relative_to_all_tumor"
                                  "immune_tumor_antigen_fractions"
                                  "immune_cell_functional_relative_to_all_tumor"
                                  "immune_cell_func_tumor_antigen_fractions"]]
   ;; green
   ["immune_cell_features" ["immune_cell_functional_relative_to_all_immune"                            
                            "immune_cell_relative_to_all_immune"
                            "immune_cell_fractions"
                            "immune_functional_marker_fractions"
                            "immune_cell_functional_spatial_density"
                            "immune_cell_spatial_density"                            
                            ]]
   ])

(defn select-widget
  [id values]
  (when-not (empty? values)
    (rf/dispatch [:set-param-if :universal id (name (first values))])) ;TODO smell? But need to initialize somewhere
  [:div.row
   [:div.col-4 [:label.small [:b id]]]  ;TEMP for dev I think
   [:div.col-4
    (wu/select-widget
     id
     @(rf/subscribe [:param :universal id])
     #(rf/dispatch [:set-param :universal id %])
     (map (fn [v] {:value v :label (wu/humanize v)}) values)
     nil)]])

;;; TODO this is not right, should filter features by meta-cluster I think?
(defn l3-feature
  []
  (select-widget :feature data/features))

(defn l2-spatial
  []
  [:i "TBD"])

(defn tumor_antigen_co_relative
  []
  [:div
   (select-widget :feature-antigen-1 ["TBD"])
   (select-widget :feature-antigen-2 ["TBD"])
   (select-widget :feature-relative ["Antigen-1" "Antigen-2" "Total"]) ;TODO should adapt selections to specific antigetns
   ])

(defn l2-nonspatial
  []
  [:div 
   (select-widget :feature-l2 (map first non-spatial-features-2-3))
   (let [feature-l2 @(rf/subscribe [:param :universal :feature-l2])]
     (if (= "marker_intensity" feature-l2)
       [:div
        (select-widget :feature-l3-meta-cluster cell-meta-clusters)
        [l3-feature]]
       ;; Not marker_intensity
       [:div
        (select-widget :feature-l3-bio-feature-type (get (into {} non-spatial-features-2-3) feature-l2))
        (when-let [bio_feature_type @(rf/subscribe [:param :universal :feature-l3-bio-feature-type])]
          (when-let [l4-features @(rf/subscribe [:data [:features {:bio_feature_type bio_feature_type}]])]
            (select-widget :feature-l4-feature l4-features)))]))])

(defn ui
  []
  [:div.row
   [:div.col-6
    [:h4 "Feature Selection (work in progress)"]
    [select-widget :feature-l1 [:non-spatial :spatial]]
    (if (= "non-spatial" @(rf/subscribe [:param :universal :feature-l1]))
      [l2-nonspatial]
      [l2-spatial])]])

