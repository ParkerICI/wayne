(ns wayne.data
  (:require [wayne.bigquery :as bq]
            [taoensso.timbre :as log]
            [org.candelbio.multitool.core :as u]))

(defn query
  [q]
  (bq/query "pici-internal" q))

;;; Not only for DRY purposes, reduces impact of SQL injection attacks
(defn select
  [q & {:keys [] :as args}]
  (bq/query "pici-internal"
            (u/expand-template
             (str "select " q)
             (merge args
                    {:from " FROM `pici-internal.bruce_external.feature_table` "})
             :key-fn keyword)))

(defn sites
  []
  (map :site (select "distinct site {from} ")))

(defn feature-types
  []
  (map :feature_type (select "distinct feature_type {from} ")))

(def sites '("CoH" "CHOP" "UCLA" "UCSF" "Stanford"))

;;; 14K features! Kind of useless
(defn features
  [site type]
  (map :feature_variable
       (select
        "distinct feature_variable {from} where site = '{site}' and feature_type = '{type}'" :site site :type  type)))

(defn features+
  [site]
  (select "distinct feature_variable, feature_type {from} where site = '{site}'"  :site site))

(defn feature-count
  []
  (select "site, feature_type, count(distinct(feature_variable)) {from} group by site, feature_type"))





#_(api {:site "Stanford" :feature "CD86"})

;;; curl "http://localhost:1088/api/v2/data0?site=Stanford&feature=CD86"


;;; Patient and sample


(comment
  (defn samples-per-patient
    []
    (select "patient_id, count(distinct(sample_id)) {from} group by patient_id"))

  (def p0 (query "select * from `pici-internal.bruce_external.feature_table` where patient_id = '49178'"))

  (zipmap (keys (first p0)) (map #(count (distinct (map (fn [row] (get row %)) p0))) (keys (first p0))))
  {:patient_id 1,
   :group 1,
   :ROI 2,
   :site 1,
   :immunotherapy 1,
   :feature_type 8,
   :feature_value 3011,
   :cell_meta_cluster_final 22,
   :who_grade 1,
   :fov 3,
   :sample_id 3,
   :int64_field_0 16551,
   :feature_source 2,
   :recurrence 1,
   :cohort 1,
   :final_diagnosis 1,
   :feature_variable 4488,
   :progression 1}
  )

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
  (select "site, count(distinct(patient_id)) as patients, count(distinct(sample_id)) as samples {from} group by site"))


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


(defn data
  [dataset]
  (log/info :data dataset)
  (case dataset
    "patients" (patient-table)
    "sites" (site-table)
    "samples" (sample-table)))

;;; For violins

(defn clean-data
  [d]
  (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v)))))
       d))

(defn query0
  [{:keys [site feature]}]
  (select "ROI, immunotherapy, feature_value {from} 
where 
site = '{site}' 
and final_diagnosis = 'GBM' 
and feature_variable = '{feature}' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
" :site site :feature feature))

(defn data0
  [params]
  (-> (query0 params)
      clean-data))
