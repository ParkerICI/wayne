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
   {:table metadata-table}))

;;; Grid


(u/def-lazy grid-data
  (u/forcat [dim1 (keys dd/dims)
             dim2 (keys dd/dims)]
    (map (fn [row]
           (-> row
               (update :dim1 #(str (name dim1) ": " %))
               (update :dim2 #(str (name dim2) ": " %))
               ))
         (select "{{dim1}} as dim1, {{dim2}} as dim2, count(distinct(patient_id)) as count {{from}} group by {{dim1}}, {{dim2}}" {:dim1 (name dim1) :dim2 (name dim2)})

         )))

(defmethod wd/data :grid
  [_]
  @grid-data)


