(ns wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [com.hyperphor.way.web-utils :as wu]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.autocomplete :as autocomplete] ;TEMP
            [wayne.frontend.utils :as wwu]
            )
  )

;;; ⊛✪⊛ Data defintions ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

;;; To generate these, see scrap/data-curation.
;;; TODO might want to move to wayne.data-defs

;;; Feature tree generation (select widget hierarchy etc) 

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

;;; Values for segmented feature selectors

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
    ]
   ["B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA"]]
  )

(def neighborhoods
  ["APC"
   "Bcells"
   "DC_Mac_CD209"
   "Endothelial"
   "Endothelial_cells"
   "Immune"
   "Lymphoid"
   "Macrophage_CD206"
   "Macrophage_CD68"
   "Macrophage_CD68_CD163"
   "Mast_cells"
   "Microglia"
   "Microglia_CD163"
   "Myeloid"
   "Myeloid_CD11b"
   "Myeloid_CD11b_HLADR+"
   "Myeloid_CD14"
   "Myeloid_CD141"
   "Myeloid_CD14_CD163"
   "Neurons"
   "Neutrophils"
   "Tcell_CD4"
   "Tcell_CD8"
   "Tcell_FoxP3"
   "Tumor_cells"])

(def feature-definitions
  {"Relative_Intensity" "Mean glycan intensity / Σ (all glycan intensity)"
   "Tumor_Antigens_Intensity" "Mean cell intensity"
   "Functional_marker_intensity" "Mean cell intensity"
   "Phenotype_marker_intensity" "Mean cell intensity"
   ;; Cell Abundance – Cell abundance
   ;; Ratios – Cell ratios
   "Neighborhood_Frequencies"  "Box 1 – cell type 1. Box 2 is % of cell type 2 within 50-micron radius of cell type 1"
   "Spatial_Density" "Clustered regions of spatial enrichment. See extended doc (Data access) for cluster compositions"
   "Area_Density" "Cell counts over FOV area"
   "Immune_High" "Transcripts collected from CD45 high regions"
   "Immune_Low" "Transcripts collected from CD45 low regions"
   "Relative_to_all_tumor_cells" "Relative abundance of cell types – Denominator is total tumor cells. "
   "Relative_to_all_immune_cells" "Relative abundance of cell types – Denominator is total immune cells."
   ;; rename to “Immune & functional marker to tumor & antigen” –
   "Cells_and_functional_markers"  "X/(X + Y) –  X  = Counts of any combination of immune cells and functional marker. Y = Counts of tumor cells expressing a specific tumor antigen"
   ;; rename to “Immune to immune”
   "Immune_cells"  "X/(X + Y) –  X  = Counts of a immune cell. Y = Counts of an immune cell"
   "Immune_to_Tumor_cells" "X/(X + Y) –  X  = Counts of a immune cell. Y = Counts of an immune cell"
   ;; Rename to tumor to tumor (?)
   "Tumor_cells" "(X/Y) - Count of tumor cells coexpression 2 (Antigen A and Antigen B) tumor antigens/ Counts of tumor cells expressing either antigen A or B. X/(X+Y) – X = Counts of tumor cells expressing a specific antigen. Y = Counts of tumor cells expressing a specific antigen"
   })


;;; ⊛✪⊛ Utilities ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

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

;;; NOTE used to use :set-param-if which didn't work well, I think that can be removed from Way
(defn select-widget-minimal
  [id values & [extra-action believe-param?]]
  (let [current-value @(rf/subscribe [:param :features id])]
    (when (and (not (empty? values))
               (not (contains? (set values) current-value)))
      (rf/dispatch [:set-param :features id (safe-name (if believe-param? ;Another epicycle, ensures this works for examples where everything gets set at once. 
                                                         (or current-value (first values))
                                                         (first values)))
                                                         ]))
    [:span {:style {:width "80%"}}
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
      {:display "inherit" :width "inherit" :margin-left "2px"})
     (when-let [d (get feature-definitions current-value)]
       (wwu/info d))])) ;TODO tooltips

(defn select-widget
  [id values & [extra-action believe-param?]]
  [row
   (wu/humanize (trim-prefix id))
   (select-widget-minimal id values extra-action believe-param?)
   ])

;;; Hack and temporary

(defn clean-select-value
  [v]
  (if (= v "---")
    nil
    v))

;;; ⊛✪⊛ Segmented features ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

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

;;; Methods for customizing the bottom level of feature select tree
;;; Note: these aren't parallel becau

(defmulti feature-variable-ui (fn [feature-type bio-feature-type] [feature-type bio-feature-type]))

(defmethod feature-variable-ui :default
  [feature-type bio-feature-type]
  (select-widget :feature-feature_variable
                 @(rf/subscribe [:data :features {:feature_type feature-type
                                                  :bio_feature_type bio-feature-type}])
                 nil
                 true                   ;kludge so this works with examples
                 ))

(defmulti feature-from-db
  (fn [db]
    [(get-in db [:params :features :feature-feature_type])
     (get-in db [:params :features :feature-bio-feature-type])]))

(defmethod feature-from-db :default
  [db]
  (get-in db [:params :features :feature-feature_variable]))

(defmethod feature-variable-ui ["Immune_High" nil]
  [_ _]
  (row "RNA" [autocomplete/ui]))

(defmethod feature-from-db :default
  [db]
  (get-in db [:params :features :feature-feature_variable]))

(defmethod feature-variable-ui ["Immune_Low" nil]
  [_ _]
  (row "RNA" [autocomplete/ui]))

(defmethod feature-variable-ui ["Cell_Ratios" "Cells_and_functional_markers"]
  [_ _]
  (row "feature_variable" 
       [segmented [(first cells-and-functional-marker-segs)
                   "over"
                   (wu/humanize @(rf/subscribe [:param :features :subfeature-0]))
                   "plus"
                   (second cells-and-functional-marker-segs)
                   ]]))

(defn joins
  [& segs]
  (str/join "_" segs))

(defmethod feature-from-db ["Cell_Ratios"  "Cells_and_functional_markers"]
  [db]
  (joins (get-in db [:params :features :subfeature-0])
         "over"
         (get-in db [:params :features :subfeature-0])
         "plus"
         (get-in db [:params :features :subfeature-4])
         "func"))

(defmethod feature-variable-ui ["Neighborhood_Frequencies" nil]
  [_]
  (row "feature_variable"
       [segmented
        [neighborhoods
         "-"
         neighborhoods]]))

(defmethod feature-from-db ["Neighborhood_Frequencies" nil]
  [db]
  (str (get-in db [:params :features :subfeature-0])
       "-"
       (get-in db [:params :features :subfeature-2])))
    
;;; ⊛✪⊛ UI and data ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

(rf/reg-sub
 :selected-feature
 (fn [db _]
   ;; stuff into query
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
        l4-feature (if (empty? l4-feature-tree)
                     (do (rf/dispatch [:set-param :features :feature-bio-feature-type nil]) nil)
                     @(rf/subscribe [:param :features :feature-bio-feature-type]))
        ]
    [:div
     (select-widget :feature-supertype (map first feature-tree))
     (select-widget :feature-broad_feature_type (map first l2-feature-tree))
     (select-widget :feature-feature_type (map first l3-feature-tree))
     (when-not (empty? l4-feature-tree)
       (select-widget :feature-bio-feature-type (map first l4-feature-tree)))
     (feature-variable-ui l3-feature l4-feature)
     (row "selected" @(rf/subscribe [:selected-feature]))
     ]))

(defn ui
  []
  [:div.row
   [:div.col-10
    [new-feature-selector]
    ]])

