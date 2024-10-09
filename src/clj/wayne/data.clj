(ns wayne.data
  (:require [wayne.bigquery :as bq]
            [taoensso.timbre :as log]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [org.candelbio.multitool.math :as mu]
            [com.hyperphor.way.data :as wd]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [environ.core :as env]
            ))

;;; See https://console.cloud.google.com/bigquery?authuser=1&project=pici-internal&ws=!1m0
;;; or
;;; ⪢ gcloud alpha bq tables list --dataset bruce_external



#_
(def bq-table (env/env :bq-data-table "pici-internal.bruce_external.feature_table_20240409"))

;;; New data table 
(def bq-table (env/env :bq-data-table "pici-internal.bruce_external.feature_table_20240810_metadata_oct1"))

(defn query
  [q]
  (bq/query "pici-internal" q))

;;; q is a template with {from} to plae the FROM clause. Kind of confusing
(defn select
  [q & {:keys [] :as args}]
  (bq/query "pici-internal"
            (u/expand-template
             (str "select " q)
             (merge args
                    {:from (format " FROM `%s` " bq-table)})
             :key-fn keyword)))

(defmethod wd/data :cohort
  [_]
  (select "Tumor_Diagnosis,
count(distinct(patient_id)) as patients,
count(distinct(sample_id)) as samples,
count(distinct(feature_variable)) as features
{from} group by Tumor_Diagnosis"))

(defmethod wd/data :samples
  [_]
  (select "sample_id, 
any_value(patient_id) as patient_id, 
any_value(WHO_grade) as WHO_grade,
any_value(Tumor_Diagnosis) as Tumor_Diagnosis,
any_value(Recurrence) as Recurrence,
any_value(immunotherapy) as immunotherapy,
{from}
group by sample_id"))


;;; Sketch towards the patient table in Munson design
;;; Not actually called yet, and needs more fields
(defmethod wd/data :patients
  [_]
  (select "patient_id,
array_agg(distinct sample_id) as sample_id,
any_value(WHO_grade) as WHO_grade
{from}
group by patient_id"))


(defn clean-data
  [d]
  (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v)))))
       d))

;;; General trick to convert maps in get params back into their real form.
;;; → Way
(defn params-remap
  [params]
  (reduce-kv (fn [params k v]
               (if (= v "null")         ;fix "null"s
                 (dissoc params k)      
                 (if (and (string? k) (re-find #"\[.*\]" k)) 
                   (dissoc (assoc-in params (mapv keyword (re-seq #"\w+" k)) v) k) ;TODO a bit hacky
                   params)))
             params params))

(defn true-values
  [map]
  (u/forf [[k v] map]
    (when (= v "true") (name k))))

;;; Generate a where clause from a field/value map:
;; {:Tumor_Diagnosis {:GBM "true", :Astrocytoma "true"}, :recurrence {:yes "true"}})
;;; => "Tumor_Diagnosis in ('GBM', 'Astrocytoma') AND recurrence in ('yes')"
(defn joint-where-clause
  [values-map]
  (if (empty? values-map)
    "true"
    (str/join
     " AND "
     (when values-map
       (for [dim (keys values-map)]
         (let [vals (true-values (get values-map dim))]
           (if (empty? vals)
             "1 = 1"
             (format "%s in %s" (name dim) (bq/sql-lit-list vals)))))))))

(defn query1
  [{:keys [feature dim filters]}]
  (when (and feature dim)
    (-> (select "feature_value, {dim} {from} 
where feature_variable = '{feature}' AND {where}" ; tried AND feature_value != 0 but didn't make a whole lot of differe
                :dim dim
                :feature feature
                :where (str (joint-where-clause (dissoc filters (keyword feature)))  )) ; " and cell_meta_cluster_final = 'APC'"
        clean-data)))

;;; Allowable feature values for a single dim, given feature and filters
;;; TODO should delete dim from filters, here or upstream
(defn query1-pop
  [{:keys [dim feature filters] :as params}]
  (when dim
    (->>
     (select "distinct {dim} {from} where {feature-clause} AND {where}"
             (assoc params
                    :feature-clause (if feature (u/expand-template "feature_variable = '{feature}'" {:feature feature} :key-fn keyword) "1=1")
                    :where (joint-where-clause (dissoc filters (keyword dim)))))
     (map (comp second first))
     set)))

(defn heatmap
  [{:keys [dim filter]}]
  (when dim
    (-> (select "avg(feature_value) as mean, feature_variable, {dim} {from} 
 where feature_type = 'marker_intensity' and {where}
 group by feature_variable, {dim}"
                :dim dim
                :where (joint-where-clause filter))
        )))

;;; Feature-list driven
(defn heatmap2
  [{:keys [dim feature-list filter]}]
  (let [feature-list (cond
                       (string? feature-list) (vector feature-list)
                       (vector? feature-list) feature-list
                       :else [])]
    (when (and dim (not (empty? feature-list)))
      (-> (select "avg(feature_value) as mean, feature_variable, {dim} {from} 
 where feature_variable in {feature-list}  and {where}
 group by feature_variable, {dim}"
                  :feature-list (bq/sql-lit-list feature-list)
                  :dim dim
                  :where (joint-where-clause filter))
          ))))

(u/defn-memoized bio_feature_type-features
  [bio_feature_type]
  (map :feature_variable                ;(comp patch-feature
       (select "distinct feature_variable {from} where bio_feature_type = '{bio_feature_type}'"
               :bio_feature_type bio_feature_type)))

(defmethod wd/data :universal
  [params]
  (query1 (params-remap params)))

(defmethod wd/data :populate
  [params]
  (query1-pop (params-remap params)))

(defmethod wd/data :features
  [params]
  (bio_feature_type-features (:bio_feature_type params)))

(defmethod wd/data :heatmap
  [params]
  (heatmap (params-remap params)))

(defmethod wd/data :heatmap2
  [params]
  (heatmap2 (params-remap params)))

;;; curl "http://localhost:1199/api/data?data-id=rna-autocomplete&prefix=CL"

;;; Note: in theory, might want to be parameterizable by feature_type = Immune_High or _Low, but in practice these are the same
(defmethod wd/data :rna-autocomplete
  [params]
  (map :feature_variable
       (select "distinct(feature_variable) {from}
where bio_feature_type = 'spatial_RNA'
and feature_variable like '{prefix}%%'  order by feature_variable limit 20"
               params)))
