(ns wayne.data-prep
  (:require [wayne.data :as d])
  )

;;; This file is for various data munging things that are not part of the running app,
;;; BUT will need to work when new data is released.

;;; Repeated in universal.cljs
(def grouping-features [:final_diagnosis :who_grade :ROI :recurrence
                        :treatment :idh_status])

;;; New = 20240810
(def new-grouping-features [:final_diagnosis :who_grade  :recurrence :idh_status
                            :tumor_region ; for :ROI
                            :final_diagnosis_simple ; for :treatment, but that can't be right
                            ])

(defn query1-meta
  [{:keys [feature filters] :as params}]
  ;; TODO conditionalize to avoid prod errors
  (when feature
    (d/select "distinct {dim} {from} 
where {where}
"
            :dim (name feature)
            :where (d/joint-where-clause (dissoc filters (keyword feature))))
    ))

;;; Generate a map of filter dims and values, pasted by hand into front end for now
(defn generate-filters
  [features]
  (zipmap features
          (map #(sort (map (comp second first) (query1-meta {:feature % :filters {}})))
               features)))

;;; Old table
#_ (generate-table grouping-features)
{:final_diagnosis
 ("Astrocytoma"
  "Diffuse_midline_glioma"
  "GBM"
  "Ganglioglioma"
  "Glioma"
  "Normal_brain"
  "Oligodendroglioma"
  "PXA"
  "Thalmic_glioma"
  "pGBM"
  "pHGG"),
 :who_grade ("2" "3" "4" "NA"),
 :ROI
 ("INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR" "SOLID_TUMOR" "TUMOR" "other"),
 :recurrence ("no" "unknown" "yes"),
 :treatment
 ("CHOP_brain_cptac_2020"
  "CHOP_openpbta"
  "CHOP_pbta_all"
  "CHOP_unknown"
  "CoH_control"
  "CoH_neoadjuvant"
  "Stanford_unknown"
  "UCLA_control"
  "UCLA_neoadjuvant_nonresp"
  "UCLA_neoadjuvant_resp"
  "UCSF_0"
  "UCSF_lys_control"
  "UCSF_neoadjuvant_SPORE_CD27"
  "UCSF_neoadjuvant_SPORE_vaccine"
  "UCSF_neoadjuvant_lys_vaccine"
  "UCSF_non_trial_controls"
  "UCSF_pre_trial"
  "UCSF_pxa_group"),
 :idh_status ("NA" "mutant" "unknown" "wild_type")}


;;; New table
#_ (generate-table new-grouping-features)
{:final_diagnosis
 ("Astrocytoma"
  "Breast_CA"
  "Colon_CA"
  "FCD"
  "FTC"
  "GBM"
  "Meningioma"
  "NA"
  "Normal_brain"
  "Oligodendroglioma"
  "PXA"
  "Placenta"
  "Tonsil"
  "VM"
  "gliosis"
  "mFTC_brain"
  "pAstrocytoma"
  "pDiffuse_midline_glioma"
  "pGBM"
  "pGanglioglioma"
  "pGlioma"
  "pHGG"
  "pPXA"
  "pThalmic_glioma"),
 :who_grade ("2" "3" "4" "NA" "unknown"),
 :recurrence ("NA" "no" "unknown" "yes"),
 :idh_status ("NA" "mutant" "unknown" "wild_type"),
 :tumor_region ("NA" "other" "tumor_core" "tumor_core_to_infiltrating" "tumor_infiltrating"),
 :final_diagnosis_simple
 ("Astrocytoma" "GBM" "NA" "Oligodendroglioma" "PXA" "Pediatric DIPG" "Pediatric HGG (other)")}
