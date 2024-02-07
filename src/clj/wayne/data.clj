(ns wayne.data
  (:require [wayne.bigquery :as bq]
            [org.candelbio.multitool.core :as u]))

(defn query
  [q]
  (bq/query "pici-internal" q))

(defn query0
  [{:keys [site feature]}]
  (format "SELECT ROI, immunotherapy, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = '%s' 
and final_diagnosis = 'GBM' 
and feature_variable = '%s' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
" site feature))

(defn sites
  []
  (map :site (query "select distinct site from `pici-internal.bruce_external.feature_table` ")))

(defn feature-types
  []
  (map :feature_type (query "select distinct feature_type from `pici-internal.bruce_external.feature_table` ")))

(def sites '("CoH" "CHOP" "UCLA" "UCSF" "Stanford"))

;;; 14K features! Kind of useless
(defn features
  [site type]
  (map :feature_variable
       (query
        (format "select distinct feature_variable from `pici-internal.bruce_external.feature_table` where site = '%s' and feature_type = '%s'" site type))))

(defn features+
  [site]
  (query (format  "select distinct feature_variable, feature_type  from `pici-internal.bruce_external.feature_table` where site = '%s'" site)))

(defn feature-count
  []
  (query
   "select site, feature_type, count(distinct(feature_variable)) from `pici-internal.bruce_external.feature_table` group by site, feature_type"))

(defn clean-data
  [d]
  (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v)))))
       d))

(defn data0
  [params]
  (let [q (query0 params)]
    (prn :data params q)
    (->> (query q)
         clean-data)))

#_(api {:site "Stanford" :feature "CD86"})

;;; curl "http://localhost:1088/api/v2/data0?site=Stanford&feature=CD86"


;;; Patient and sample

(defn patients
  []
  (query "select distinct patient_id, site from `pici-internal.bruce_external.feature_table`"))

(defn samples-per-patient
  []
  (query "select patient_id, count(distinct(sample_id)) from `pici-internal.bruce_external.feature_table` group by patient_id"))


(comment
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

(defn patient-table
  []
  ;; final_diagnosis and who_grade can have multiple values, so leaving out the former
  (query "select distinct patient_id, site, `group`, cohort, immunotherapy, who_grade, recurrence, progression from `pici-internal.bruce_external.feature_table`"))
