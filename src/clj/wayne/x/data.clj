(ns wayne.x.data
  (:require [wayne.bigquery :as bq]
            [wayne.data :refer :all]
            [wayne.data-defs :as dd]
            [taoensso.timbre :as log]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [org.candelbio.multitool.math :as mu]
            [com.hyperphor.way.data :as wd]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [environ.core :as env]
            [clojure.java.io :as io]
            [wayne.csv :as csv]
            ))

(defmethod wd/data :cohort
  [_]
  (select "Tumor_Diagnosis,
count(distinct(patient_id)) as patients,
count(distinct(sample_id)) as samples,
count(distinct(feature_variable)) as features
{{from}} group by Tumor_Diagnosis"))

(defmethod wd/data :samples
  [_]
  (select "patient_id, sample_id, who_grade, final_diagnosis_simple, immunotherapy, site {{from}}" {:table metadata-table}))

(defmethod wd/data :patients
  [_]
  (select "patient_id,
array_agg(sample_id) as samples,
any_value(who_grade) as who_grade,
any_value(final_diagnosis_simple) as diagnosis,
any_value(immunotherapy) as immunotherapy,
any_value(site) as site,
any_value(sex) as sex,
{{from}} group by patient_id"
          {:table metadata-table})
  )

;;; Full metadata
(defmethod wd/data :metadata
  [_]
  (select "* {{from}}" {:table metadata-table}))

(defmethod wd/data :sites
  [_]
  (select
   "site,
count(distinct(patient_id)) as patients,
count(distinct(sample_id)) as samples
 {{from}}  group by site"
   {:table metadata-table})))


