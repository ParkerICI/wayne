(ns org.parkerici.wayne.frontend.data
  )

;;; Global col-defs, might make more sense to do these per-view
(def col-defs
  {:sample_id {:url-template "sample/%s"}
   :samples {:url-template "sample/%s"}
   ; :site {:url-template "site/%s"}
   ; :patient_id {:patient_id "patient/%s"}
   })

(def sites ["CoH" "CHOP" "UCLA" "UCSF" "Stanford"])

(def features
  ["Olig2"
   "CD133"
   "EphA2"
   "CD31"
   "CD47"
   "CD38"
   "HLADR"
   "CD45"
   "CD4"
   "CD8"
   "CD86"
   "PD1"
   "CD14"
   "Ki67"
   "NG2"
   "H3K27me3"
   "ICOS"
   "CD3"
   "H3K27M"
   "LAG3"
   "B7H3"
   "CD11b"
   "GFAP"
   "NeuN"
   "IDO1"
   "IDH1_R132H"
   "TIM3"
   "GM2_GD2"
   "TMEM119"
   "CD70"
   "CD40"
   "Tox"
   "CD141"
   "CD209"
   "EGFR"
   "CD206"
   "FOXP3"
   "Calprotectin"
   "HLA1"
   "EGFRvIII"
   "ApoE"
   "CD123"
   "GLUT1"
   "CD163"
   "Chym_Tryp"
   "GPC2"
   "CD20"
   "CD208"
   "FoxP3"
   "HER2"
   "VISTA"
   "CD68"
   "PDL1"])



(def values-d
  {:patient_id 268,
   :group ["unknown" "A" "B" "C" "D"],
   :Tumor_Region
   ["other" "TUMOR" "SOLID_TUMOR" "INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR"],
   :site ["CoH" "CHOP" "UCLA" "UCSF" "Stanford"],
   :immunotherapy ["true" "false"],
   :feature_type
   ["intensity"
    "tumor_cell_ratios"
    "immune_cell_ratios"
    "immune_func_ratios"
    "tumor_cell_density"
    "immune_cell_density"
    "immune_func_density"
    "immune_tumor_cell_ratios"
    "immune_func_ratios_to_all"],
   :source_table ["cell_table_immune" "cell_table_tumor" "NA"],
   :feature_value 218965,
   :cell_meta_cluster_final
   ["Endothelial_cells"
    "Myeloid_CD14"
    "Tumor_cells"
    "APC"
    "Neurons"
    "Macrophage_CD163"
    "Immune_unassigned"
    "Tcell_CD8"
    "Neutrophils"
    "Macrophage_CD68"
    "Microglia"
    "Unassigned"
    "Macrophage_CD206"
    "Myeloid_CD11b"
    "DC_CD123"
    "Myeloid_CD141"
    "Tcell_FoxP3"
    "DC_Mac_CD209"
    "DC_CD206"
    "Bcells"
    "NA"
    "Tcell_CD4"
    "Mast_cells"],
   :treatment
   ["CoH_neoadjuvant"
    "CoH_control"
    "CHOP_unknown"
    "CHOP_pbta_all"
    "UCLA_control"
    "UCLA_neoadjuvant_resp"
    "UCSF_pre_trial"
    "UCSF_0"
    "UCSF_neoadjuvant_lys_vaccine"
    "CHOP_openpbta"
    "CHOP_brain_cptac_2020"
    "UCLA_neoadjuvant_nonresp"
    "UCSF_lys_control"
    "UCSF_neoadjuvant_SPORE_CD27"
    "UCSF_neoadjuvant_SPORE_vaccine"
    "UCSF_non_trial_controls"
    "UCSF_pxa_group"
    "Stanford_unknown"],
   :WHO_grade ["4" "NA" "2" "3"],
   :sample_id 590,
   :IDH_R132H_Status ["wild_type" "unknown" "mutant" "NA"],
   :int64_field_0 350000,
   :feature_source ["cell_meta_cluster_final" "whole_sample"],
   :recurrence ["yes" "unknown" "no"],
   :cohort
   ["neoadjuvant"
    "control"
    "unknown"
    "pbta_all"
    "neoadjuvant_resp"
    "pre_trial"
    "0"
    "neoadjuvant_lys_vaccine"
    "openpbta"
    "brain_cptac_2020"
    "neoadjuvant_nonresp"
    "lys_control"
    "neoadjuvant_SPORE_CD27"
    "neoadjuvant_SPORE_vaccine"
    "non_trial_controls"
    "pxa_group"],
   :feature ["non_spatial"],
   :Tumor_Diagnosis
   ["GBM"
    "Astrocytoma"
    "PXA"
    "Oligodendroglioma"
    "Normal_brain"
    "pGBM"
    "Thalmic_glioma"
    "Glioma"
    "pHGG"
    "Diffuse_midline_glioma"
    "Ganglioglioma"],
   :feature_variable 18582,
   :progression ["unknown" "no" "no_later_event" "yes_later_event" "yes"]})




;;; NOTE: still needs work, particularly separating cell types from features in some areas
;;; Generally if there is only one size, we're good.
;;; From clj/data/ui-master
;;; PATCHED by hand in a few cases, which are marked
;;; PATCHED fix count_ errors
(def feature-ui-master
  '{"tumor_antigen_fractions"
    {11
     [("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("over")
      ("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("counts")
      ("plus")
      ("EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("counts")
      ("prop")]},
    "immune_cell_relative_to_all_tumor"
    {4
     [("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("over")
      ("all_tumor_count")
      ("prop")]},
    "tumor_antigen_co_relative"
    {8
     [("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2")
      ("func")
      ("EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("over")
      ("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("prop")]},
    "immune_cell_functional_relative_to_all_tumor"
    {3
     [("APC"
        "Bcells"
        "DC_CD123"
        "DC_CD206"
        "DC_Mac_CD209"
        "Immune_unassigned"
        "Macrophage_CD163"
        "Macrophage_CD206"
        "Macrophage_CD68"
        "Microglia"
        "Myeloid_CD11b"
        "Myeloid_CD14"
        "Myeloid_CD141"
        "Neurons"
        "Neutrophils"
        "Tcell_CD4"
        "Tcell_CD8"
        "Tcell_FoxP3"
        "Tumor_cells"
        "Unassigned")
       ("CD86" "ICOS" "IDO1" "Ki67" "LAG3" "PD1" "PDL1" "TIM3" "Tox" "iNOS")
      ("over")
      ("all_tumor_count")]},
    "immune_cell_functional_spatial_density"
    {2
     [("CD38" "CD86" "GLUT1" "ICOS" "IDO1" "Ki67" "LAG3" "PD1" "PDL1" "TIM3" "Tox" "iNOS")
      ("density")]},
    "immune_tumor_antigen_fractions"
    {8
     [("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("over")
      ("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("plus")
      ("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("prop")],
     7
     [("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("over")
      ("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("plus")
      ("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("prop")]},
    "immune_cell_fractions"
    {6
     [("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("over")
      ("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("plus")
      ("Bcells"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("prop")]},
    "immune_cell_functional_relative_to_all_immune"
    {3
     [("APC_CD86"
       "APC_IDO1"
       "APC_Ki67"
       "APC_PDL1"
       "APC_TIM3"
       "APC_iNOS"
       "Bcells_Ki67"
       "Bcells_PDL1"
       "DC_CD123_CD86"
       "DC_CD123_IDO1"
       "DC_CD123_Ki67"
       "DC_CD123_PD1"
       "DC_CD123_PDL1"
       "DC_CD123_TIM3"
       "DC_CD206_CD86"
       "DC_CD206_Ki67"
       "DC_CD206_PDL1"
       "DC_CD206_TIM3"
       "DC_Mac_CD209_CD86"
       "DC_Mac_CD209_IDO1"
       "DC_Mac_CD209_Ki67"
       "DC_Mac_CD209_PDL1"
       "DC_Mac_CD209_TIM3"
       "DC_Mac_CD209_iNOS"
       "Immune_unassigned_Ki67"
       "Immune_unassigned_PDL1"
       "Immune_unassigned_Tox"
       "Immune_unassigned_iNOS"
       "Macrophage_CD163_CD86"
       "Macrophage_CD163_IDO1"
       "Macrophage_CD163_Ki67"
       "Macrophage_CD163_PD1"
       "Macrophage_CD163_PDL1"
       "Macrophage_CD163_TIM3"
       "Macrophage_CD163_iNOS"
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
       "Myeloid_CD11b_CD86"
       "Myeloid_CD11b_IDO1"
       "Myeloid_CD11b_Ki67"
       "Myeloid_CD11b_PD1"
       "Myeloid_CD11b_PDL1"
       "Myeloid_CD11b_TIM3"
       "Myeloid_CD11b_iNOS"
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
       "Tcell_FoxP3_PDL1"
       "Tcell_FoxP3_TIM3"
       "Tcell_FoxP3_Tox"
       "Tumor_cells_Ki67"
       "Tumor_cells_PDL1"
       "Tumor_cells_Tox"
       "Unassigned_Ki67"
       "Unassigned_PDL1")
      ("over")
      ("all_immune_count")]},
    "immune_functional_marker_fractions"
    ;; Hand tweaked
    {8
     [("Tumor_cells"
        "Microglia"
        "Macrophage_CD206"
        "Macrophage_CD68"
        "Neutrophils"
        "DC_CD206"
        "DC_CD123"
        "Myeloid_CD141"
        "Unassigned"
        "Tcell_FoxP3"
        "Immune_unassigned"
        "Tcell_CD4"
        "Myeloid_CD11b"
        "Myeloid_CD14"
        "Tcell_CD8"
        "Bcells"
        "APC"
        "Macrophage_CD163"
        "Neurons"
        "DC_Mac_CD209"
        "Mast_Cells")
      ("IDO1" "LAG3" "Ki67" "PDL1" "PD1" "CD38" "iNOS" "Tox" "GLUT1" "TIM3" "CD86" "ICOS")
      ("over")
      ("Tumor_cells"
        "Microglia"
        "Macrophage_CD206"
        "Macrophage_CD68"
        "Neutrophils"
        "DC_CD206"
        "DC_CD123"
        "Myeloid_CD141"
        "Unassigned"
        "Tcell_FoxP3"
        "Immune_unassigned"
        "Tcell_CD4"
        "Myeloid_CD11b"
        "Myeloid_CD14"
        "Tcell_CD8"
        "Bcells"
        "APC"
        "Macrophage_CD163"
        "Neurons"
        "DC_Mac_CD209"
        "Mast_Cells")
      ("CD86" "GLUT1" "IDO1" "Ki67" "PD1" "PDL1" "TIM3" "iNOS" "CD38" "LAG3" "Tox" "ICOS")
      ("plus")
      ("a" "b")
      ("c" "d")
      ]},
    "tumor_antigen_spatial_density"
    {5
     [("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2")
      ("func")
      ("EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("density")],
     9
     [("B7H3" "EGFR" "GM2_GD2" "GPC2")
      ("func")
      ("EGFR" "GM2_GD2" "GPC2" "HER2")
      ("func")
      ("GM2_GD2" "GPC2" "HER2" "NG2")
      ("func")
      ("GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("density")],
     7
     [("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2")
      ("func")
      ("EGFR" "GM2_GD2" "GPC2" "HER2" "NG2")
      ("func")
      ("GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")
      ("func")
      ("density")],
     3 [("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA") ("func") ("density")],
     2 [("all_tumor_count") ("density")]},
    "immune_cell_func_tumor_antigen_fractions"
    {5
     [("APC"
       "Bcells"
       "DC_CD123"
       "DC_CD206"
       "DC_Mac_CD209"
       "Immune_unassigned"
       "Macrophage_CD163"
       "Macrophage_CD206"
       "Macrophage_CD68"
       "Microglia"
       "Myeloid_CD11b"
       "Myeloid_CD14"
       "Myeloid_CD141"
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Tumor_cells"
       "Unassigned")
      ("CD86" "ICOS" "IDO1" "Ki67" "LAG3" "PD1" "PDL1" "TIM3" "Tox" "iNOS")
      ("over")
      ("CD86" "ICOS" "IDO1" "Ki67" "LAG3" "PD1" "PDL1" "TIM3" "Tox" "iNOS")
      ("plus")
      ("B7H3" "EGFR" "GM2_GD2" "GPC2" "HER2" "NG2" "VISTA")]},
    "immune_cell_spatial_density"
    {2
     [("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned"
       "all_immune_count")
      ("density")]},
    "immune_cell_relative_to_all_immune"
    {4
     [("APC"
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
       "Neurons"
       "Neutrophils"
       "Tcell_CD4"
       "Tcell_CD8"
       "Tcell_FoxP3"
       "Unassigned")
      ("over")
      ("all_immune_count")
      ("prop")]}})
