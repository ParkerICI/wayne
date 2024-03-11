(ns wayne.frontend.data
  )

;;; Global col-defs, might make more sense to do these per-view
(def col-defs
  {:sample_id {:url-template "sample/%s"}
   :samples {:url-template "sample/%s"}
   ; :site {:url-template "site/%s"}
   ; :patient_id {:patient_id "patient/%s"}
   })

;;; Temp hack, these should be 

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



(def values-c
  {:patient_id 268,
   :group ["unknown" "A" "B" "C" "D"],
   :ROI
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
   :who_grade ["4" "NA" "2" "3"],
   :fov 604,
   :sample_id 590,
   :idh_status ["wild_type" "unknown" "mutant" "NA"],
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
   :final_diagnosis
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
   :progression ["unknown" "no" "no_later_event" "yes_later_event" "yes"]
   })
