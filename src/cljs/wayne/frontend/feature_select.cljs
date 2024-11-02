(ns wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [com.hyperphor.way.web-utils :as wu]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.autocomplete :as autocomplete] ;TEMP
            )
  )

;;; Data definitions

;;; Note: might be easier and less error-prone to build this from data
(def nonspatial-feature-tree
  [
   ["Cells"
    ["Cell_Abundance"
     ["Relative_to_all_tumor_cells"]
     ["Relative_to_all_immune_cells"]]
    ["Cell_Ratios"
     ["Cells_and_functional_markers"]
     ["Immune_cells"]
     ["Immune_to_Tumor_cells"]
     ["Tumor_cells"]]
    ]
   ["Protein"
    ["Tumor_Antigens_Intensity_Segments"]
    ["Tumor_Antigens_Intensity"]        ;TODO metacluster
    ["Functional_marker_intensity"]     ;ditto
    ["Phenotype_marker_intensity"]      ;dito
    ]
   ["Glycan"
    ["Relative_Intensity"]]
   ])

(def spatial-feature-tree
  [["RNA"
    ["Immune_High"]
    ["Immune_Low"]]
   ["Cells"
    ["Neighborhood_Frequencies"]          ;?db
    ["Spatial_Density"]
    ["Area_Density"
     ]]           ;db  "Cells_and_functional_markers" ?
   ]
  )

(def feature-tree
  `[["nonspatial" ~@nonspatial-feature-tree]
    ["spatial" ~@spatial-feature-tree]
    ])

;;; Building these in due to laziness. To generate, see scrap/data[-curation]
(def cells-and-functional-marker-segs
  [["APC_CD86"
    "APC_IDO1"
    "APC_Ki67"
    "APC_PD1"
    "APC_PDL1"
    "APC_TIM3"
    "APC_iNOS"
    "Bcells_Ki67"
    "Bcells_PDL1"
    "DC_Mac_CD209_CD86"
    "DC_Mac_CD209_IDO1"
    "DC_Mac_CD209_Ki67"
    "DC_Mac_CD209_PD1"
    "DC_Mac_CD209_PDL1"
    "DC_Mac_CD209_TIM3"
    "DC_Mac_CD209_iNOS"
    "Endothelial_cells_GLUT1"
    "Endothelial_cells_Ki67"
    "Endothelial_cells_PDL1"
    "Immune_unassigned_Ki67"
    "Immune_unassigned_PDL1"
    "Immune_unassigned_Tox"
    "Immune_unassigned_iNOS"
    "Macrophage_CD206_CD86"
    "Macrophage_CD206_IDO1"
    "Macrophage_CD206_Ki67"
    "Macrophage_CD206_PD1"
    "Macrophage_CD206_PDL1"
    "Macrophage_CD206_TIM3"
    "Macrophage_CD206_iNOS"
    "Macrophage_CD68_CD86"
    "Macrophage_CD68_IDO1"
    "Macrophage_CD68_Ki67"
    "Macrophage_CD68_PD1"
    "Macrophage_CD68_PDL1"
    "Macrophage_CD68_TIM3"
    "Macrophage_CD68_iNOS"
    "Microglia_CD86"
    "Microglia_IDO1"
    "Microglia_Ki67"
    "Microglia_PD1"
    "Microglia_PDL1"
    "Microglia_TIM3"
    "Microglia_iNOS"
    "Myeloid_CD141_IDO1"
    "Myeloid_CD141_Ki67"
    "Myeloid_CD141_PDL1"
    "Myeloid_CD141_TIM3"
    "Myeloid_CD141_iNOS"
    "Myeloid_CD14_CD86"
    "Myeloid_CD14_IDO1"
    "Myeloid_CD14_Ki67"
    "Myeloid_CD14_PD1"
    "Myeloid_CD14_PDL1"
    "Myeloid_CD14_TIM3"
    "Myeloid_CD14_iNOS"
    "Neurons_Ki67"
    "Neurons_PDL1"
    "Neutrophils_Ki67"
    "Neutrophils_PDL1"
    "Tcell_CD4_ICOS"
    "Tcell_CD4_Ki67"
    "Tcell_CD4_LAG3"
    "Tcell_CD4_PD1"
    "Tcell_CD4_PDL1"
    "Tcell_CD4_TIM3"
    "Tcell_CD4_Tox"
    "Tcell_CD8_ICOS"
    "Tcell_CD8_Ki67"
    "Tcell_CD8_LAG3"
    "Tcell_CD8_PD1"
    "Tcell_CD8_PDL1"
    "Tcell_CD8_TIM3"
    "Tcell_CD8_Tox"
    "Tcell_FoxP3_ICOS"
    "Tcell_FoxP3_Ki67"
    "Tcell_FoxP3_LAG3"
    "Tcell_FoxP3_PD1"
    "Tcell_FoxP3_PDL1"
    "Tcell_FoxP3_TIM3"
    "Tcell_FoxP3_Tox"
    "Tumor_cells_Ki67"
    "Tumor_cells_PDL1"
    "Tumor_cells_Tox"
    "Unassigned_Ki67"
    "Unassigned_PDL1"]
   ["B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA"]]
  )

;;; Complex enough to get its own file

(defn trim-prefix
  [id]
  (subs (name id) (count "feature-")))

(defn row
  [label contents]
  [:div.row.my-2
   [:div.col-3 [:label.small.pt-2 [:b label]]]  ;TEMP for dev I think
   [:div.col-7 contents]])

(defn safe-name
  [thing]
  (when thing
    (or (.-name thing)
        thing)))

;;; TODO nil should be option (could be in values
;;; NOTE used to use :set-param-if which didn't work well, I think that can be removed from Way
(defn select-widget-minimal
  [id values & [extra-action]]
  (let [current-value @(rf/subscribe [:param :features id])]
    (when (and (not (empty? values))
               (not (contains? (set values) current-value)))
      (rf/dispatch [:set-param :features id (safe-name (first values)) ])) 
    (wu/select-widget
     id
     current-value
     #(do
        (rf/dispatch [:set-param :features id %])
        (when extra-action (extra-action %) )) ;ugn
     (map (fn [v]
            {:value v :label (if v (wu/humanize v)  "---")})
          values)
     nil
     nil
     {:display "inherit" :width "inherit" :margin-left "2px"}))) ;TODO tooltips

(defn select-widget
  [id values & [extra-action]]
  [row
   (wu/humanize (trim-prefix id))
   (select-widget-minimal id values extra-action)])

;;; Hack and temporary

(defn clean-select-value
  [v]
  (if (= v "---")
    nil
    v))



(defn boilerplate
  [s]
  [:span.mx-2.text-nowrap s])

(defn subfeature-selector
  [subfeatures i]
  (let [;; subfeatures (subfeature-values feature-type template-position)
        ;; subfeatures (if nullable? (cons nil subfeatures) subfeatures)
        ;; Encodes the position in the parameter keyword for later extraction. Hacky but simple.
        param-key (u/keyword-conc :subfeature (str i))]
    (select-widget-minimal param-key subfeatures)))

(defn segmented
  [template]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (map (fn [elt i]
          (cond (string? elt) (boilerplate elt)
                (sequential? elt) [subfeature-selector elt i] ;sadly no subfeature name but it isn't used for anything anyway
                :else (str elt)))
        template
        (range))])

(defn cells-and-functional-marker-ui
  []
  [segmented [(first cells-and-functional-marker-segs)
              "over"
              (wu/humanize @(rf/subscribe [:param :features :subfeature-0]))
              "plus"
              (second cells-and-functional-marker-segs)
              ]])


(defn joins
  [& segs]
  (str/join "_" segs))

(defn feature-from-db
  [db]
  (case (get-in db [:params :features :feature-bio-feature-type])
    "Cells_and_functional_markers"
    (joins (get-in db [:params :features :subfeature-0])
            "over"
            (get-in db [:params :features :subfeature-0])
            "plus"
            (get-in db [:params :features :subfeature-4])
            "func")
    (get-in db [:params :features :feature-feature_variable])))    

(rf/reg-sub
 :selected-feature
 (fn [db _]
   ;; stuff into query machinery
   (let [feature (feature-from-db db)]
     (when-not (= feature (get-in db [:params :universal :feature]))
       (rf/dispatch [:set-param :universal :feature feature]))
     feature)))

(defn new-feature-selector
  []
  (let [l1-feature @(rf/subscribe [:param :features :feature-supertype])
        l2-feature-tree (rest (u/some-thing #(= (first %) l1-feature) feature-tree))
        l2-feature @(rf/subscribe [:param :features :feature-broad_feature_type])
        l3-feature-tree (rest (u/some-thing #(= (first %) l2-feature) l2-feature-tree))
        l3-feature @(rf/subscribe [:param :features :feature-feature_type])
        l4-feature-tree (rest (u/some-thing #(= (first %) l3-feature) l3-feature-tree))
        l4-feature (and (not (empty? l4-feature-tree))
                        @(rf/subscribe [:param :features :feature-bio-feature-type]))
        query-feature (or l4-feature l3-feature)
        selected-feature @(rf/subscribe [:selected-feature])
        ]
    [:div
     (select-widget :feature-supertype (map first feature-tree))
     (select-widget :feature-broad_feature_type (map first l2-feature-tree))
     (select-widget :feature-feature_type (map first l3-feature-tree))
     (when-not (empty? l4-feature-tree)
       (select-widget :feature-bio-feature-type (map first l4-feature-tree)))
     (case query-feature
       "Immune_High" (row "RNA" [autocomplete/ui])
       "Immune_Low" (row "RNA" [autocomplete/ui])
       "Cells_and_functional_markers" (row "feature_variable" [cells-and-functional-marker-ui])
       (select-widget :feature-feature_variable @(rf/subscribe [:data :features {:bio_feature_type query-feature}])))
     (row "selected" selected-feature)
     ]) )


;;; → Multitool?
(defn conjs
  [coll thing]
  (if (nil? coll)
    #{thing}
    (conj coll thing)))


(defn feature-list-ui
  []
  (let [feature @(rf/subscribe [:selected-feature])
        feature-list @(rf/subscribe [:param :heatmap2 :feature-list])]
    [:div
     [row "actual variable"
      [:span
       (wu/humanize feature)
       (when (not (contains? feature-list feature))
         [:button.btn.btn-sm.btn-secondary.mx-2 ;TODO none of these boostrap stules are present
          {:href "#"
           :on-click #(rf/dispatch [:update-param :heatmap2 :feature-list conjs feature])}
          "add"])
       ]]
     ;; TODO lozenge UI
     [row
      [:span "feature list "
       (when-not (empty? feature-list)
         [:button.btn.btn-sm.btn-secondary.mx-2 {:href "#" :on-click #(rf/dispatch [:set-param :heatmap2 :feature-list #{}])} "clear"])]
      (str/join ", " (map wu/humanize feature-list))]]))

(defn ui
  []
  [:div.row
   [:div.col-10
    [new-feature-selector]
    ]])

