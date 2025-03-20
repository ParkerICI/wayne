(ns wayne.data-defs
  )

(def dims
  (array-map                            ; Order is important 
   :Tumor_Diagnosis {:label "Tumor Diagnosis"
                     :info "Glial tumor subtypes"
                     :icon "diagnosis-icon.svg"
                     :values ["Astrocytoma"
                              "GBM"
                              "Oligodendroglioma"
                              "PXA"
                              "Pediatric DIPG"
                              "Pediatric HGG (other)"]}
   :WHO_grade {:label "WHO Grade"
               :info "World Health Organization tumor grade classification"
               :icon "question-icon.svg"
               :values  ["2" "3" "4" "Unknown"]}
   :Immunotherapy {:label "Immunotherapy"
                   :type :boolean
                   :icon "cell-therapy-2.png"
                   :info "Treatment status"
                   :values [["false" "No"] ["true" "Yes"]]}
   :treatment {:label "Treatment"
               :info "Various pre-sample collection treatments"
               :icon "treatment-icon.svg"
               :values ["Combinatorial_CD27_and_SPORE_Vaccine"
                        "Lysate_Vaccine"
                        "Neoadjuvant_PD1_Trial_1"
                        "Neoadjuvant_PD1_Trial_2"
                        "SPORE_Vaccine"
                        "Treatment_Naive"]}
   :recurrence {:label "Recurrence"
                :info "Recurrent tumor"
                :icon "recurrence-icon.svg"
                :values ["No" "Yes"]}
   :Longitudinal {:label "Longitudinal"
                  :icon "time_b.png"
                  :info "Patient samples with paired primary and recurrent events"
                  :values ["Yes" "No"]}
   :Progression {:label "Progression"
                 :icon "data.png"
                 :info "Patients that progressed from lower grade to higher grades. later event catagories denotes the recurrent tumor."
                 :values ["No" "No_later_event" "Yes" "Yes_later_event"]}
   :Tumor_Region {:label "Tumor Region"
                  :info "Anatomical regions of the tumor"
                  :icon "roi-icon.svg"
                  :values   ["Other" "Tumor_core" "Tumor_core_to_infiltrating" "Tumor_infiltrating"]}
   :IDH_R132H_Status {:label "IDH Status"
                      :info "R132H - Common IDH mutation"
                      :icon "file-chart-icon.svg"
                      :values ["Mutant" "Wild_type"]
                      }
   :Sex {:label "Sex"
         :icon "gender.png"
         :values [["F" "Female"] ["M" "Male"] "Unknown"]}
   ))

;;; Feature stuff (was in cljs/feature_select)


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

(defn- feature-tree-level
  [tree l]
  (if (= l 0)
    (map first tree)
    (feature-tree-level (mapcat rest tree) (dec l))))

(defn features
  [l]
  (vec (feature-tree-level feature-tree l)))


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
