(ns wayne.data
  (:require [way.bigquery :as bq]
            [way.utils :as wu]
            [taoensso.timbre :as log]
            [org.candelbio.multitool.core :as u]
            [clojure.string :as str]))

(defn query
  [q]
  (bq/query "pici-internal" q))

(defn select
  [q & {:keys [] :as args}]
  (bq/query "pici-internal"
            (u/expand-template
             (str "select " q)
             (merge args
                    {:from " FROM `pici-internal.bruce_external.feature_table` "})
             :key-fn keyword)))



;;; includes samples and aggregates into vector when necessary
;;; Note: this is a big pain, might want to generate it from a schema
(defn patient-table
  []
  (select "patient_id,
ANY_VALUE(site) as site, 
ANY_VALUE(`group`) as `group`, 
ANY_VALUE(cohort) as cohort, 
ANY_VALUE(immunotherapy) as immunotherapy, 
array_agg(distinct(who_grade)) as who_grade, 
array_agg(distinct(final_diagnosis)) as final_diagnosis, 
ANY_VALUE(recurrence) as recurrence, 
ANY_VALUE(progression) as progression, 
array_agg(distinct(sample_id)) as samples,
array_agg(distinct(fov)) as fovs
{from}
group by patient_id"))

;;; Sites

(defn site-table
  []
  (select "site,
count(distinct(patient_id)) as patients,
count(distinct(sample_id)) as samples,
count(distinct(feature_variable)) as features
{from} group by site"))


;;; Cohorts

(comment 
  (query "select cohort, count(distinct(patient_id)) as patients, count(distinct(sample_id)) as samples from `pici-internal.bruce_external.feature_table` group by cohort")
  )

;;;; Samples
(defn sample-table
  []
  (select "sample_id, 
any_value(patient_id) as patient_id, 
any_value(fov) as fov, 
count(1) as values, 
count(distinct(feature_variable)) as features
{from}
group by sample_id"))


(defn clean-data
  [d]
  (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v)))))
       d))

#_
(defn query0
  [{:keys [site feature]}]
  (select "ROI, immunotherapy, feature_value {from} 
where 
site = '{site}' 
and final_diagnosis = 'GBM' 
and feature_variable = '{feature}' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
" :site site :feature feature))


;;; This currently handles data for both Violin and Scatter panes

(defn query0
  [{:keys [site feature rois]}]
  (select "site, ROI, immunotherapy, feature_value, patient_id, sample_id, cell_meta_cluster_final, fov, feature_variable {from} 
where 
1 = 1
{site}
{feature}
{rois}
"
          :site (when site (format "and site = '%s'" site))
          :feature (when feature (format "and feature_variable = '%s' " feature))
          :rois (when rois (str "and ROI IN " (wu/sql-lit-list rois)))))

(defn data0
  [params]
  (-> (query0 params)
      clean-data))

(defn data
  [{:keys [data-id] :as params}]
  (log/info :data params)
  (case data-id
    "patients" (patient-table)
    "sites" (site-table)
    "samples" (sample-table)
    "dotplot" (data0 params)
    "barchart" (data0 params)
    "violin" (data0 params)
    ))

;;; Some data exploration tools, they are general so move to WAY

;;; TODO version that includes frequences
;;; TODO do these dynamically, incorporating constraints

(defn values
  [field]
  (mapv (keyword field) (select (format "distinct `%s` {from}" field))))

(defn field-frequencies
  [field]
  (let [r (select (format "count(1) as count, %s as value {from} group by %s" field field))]
    (zipmap (map :value r) (map :count r))))


(defn cols
  []
  (keys (first (select "* {from} limit 1"))))

(defn too-big
  [limit seq]
  (if (> (count seq) limit)
    (count seq)
    seq))

(defn all-values
  [limit]
  (let [fields (cols)]
    (zipmap fields
            (map (comp (partial too-big limit) values name) fields))))

;  (map :site (select "distinct site {from}")))
(def sites ["CoH" "CHOP" "UCLA" "UCSF" "Stanford"])

(def rois ["other" "TUMOR" "SOLID_TUMOR" "INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR"])

(def recurrences ["yes" "unknown" "no"])

(def progression ["unknown" "no" "yes"])

(def cohorts
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
   "pxa_group"])

(def feature_sources ["cell_meta_cluster_final" "whole_sample"])

(def group ["unknown" "A" "B" "C" "D"])

(def immunotherapy ["false" "true"])

;;; Ah this is too much trouble

(def values
  {:patient_id 268,
   :group ["unknown" "A" "B" "C" "D"],
   :ROI  ["other" "TUMOR" "SOLID_TUMOR" "INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR"],
   :site ["CoH" "CHOP" "UCLA" "UCSF" "Stanford"],
   :immunotherapy ["false" "true"],
   :feature_type
   ["intensity"
    "tumor_cell_ratios"
    "immune_cell_ratios"
    "immune_func_ratios"
    "tumor_cell_density"
    "immune_cell_density"
    "immune_func_density"
    "immune_func_ratios_to_all"],
   :feature_value 350000,
   :cell_meta_cluster_final 24,
   :who_grade ["4" "unknown" "2" "3"],
   :fov 603,
   :sample_id 590,
   :int64_field_0 350000,
   :feature_source ["cell_meta_cluster_final" "whole_sample"],
   :recurrence ["yes" "unknown" "no"],
   :cohort 16,
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
   :feature_variable 14140,
   :progression ["unknown" "no" "yes"]})
