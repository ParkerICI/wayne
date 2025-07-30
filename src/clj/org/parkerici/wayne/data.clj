(ns org.parkerici.wayne.data
  (:require [org.parkerici.wayne.bigquery :as bq]
            [org.parkerici.wayne.data-defs :as dd]
            [taoensso.timbre :as log]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [org.candelbio.multitool.math :as mu]
            [com.hyperphor.way.data :as wd]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [environ.core :as env]
            [clojure.java.io :as io]
            [org.parkerici.wayne.csv :as csv]
            ))

;;; See https://console.cloud.google.com/bigquery?authuser=1&project=pici-internal&ws=!1m0
;;; or
;;; ⪢ gcloud alpha bq tables list --dataset bruce_external

;;; was  "pici-internal.bruce_external.feature_table_20240810_metadata_oct1")

;;; Subject IDs changed. Note. used to get this through env but that turns out to be more pain that this:  (env/env :bq-data-table ...) 
(def bq-table "pici-internal.bruce_external.20240810_master_feature_table_na_removed_metadata")

;;; Used for sample table and some x pages
(def metadata-table-old "pici-internal.bruce_external.metadata_complete_feb_25_2025")
(def metadata-table "pici-internal.bruce_external.metadata_complete_2025_07_22")

(defn query
  [q]
  (bq/query "pici-internal" q))

;;; q is a template with {{from}} to place the FROM clause. Kind of confusing.
(defn select
  [q & {:keys [table] :as args :or {table bq-table}}]
  (bq/query "pici-internal"
            (u/expand-template
             (str "select " q)
             (merge args
                    {:from (format " FROM `%s` " table)})
             )))

;;; Can do deletes, etc
(defn sql
  [q & {:keys [table] :as args :or {table bq-table}}]
  (bq/query "pici-internal"
            (u/expand-template
             q
             (merge args
                    {:from (format " FROM `%s` " table)})
             )))


;;; General trick to convert maps in get params back into their real form.
;;; → Way, this should be done before methods get called 
(defn params-remap
  [params]
  (reduce-kv (fn [params k v]
               (if (= v "null")         ;fix "null"s
                 (dissoc params k)      
                 (if (and (string? k) (re-find #"\[.*\]" k))
                   (let [base (keyword (re-find   #"^\w*" k)) ;eg "filters"
                         keys (map (comp keyword second) (re-seq  #"\[(.*?)\]" k))]
                     (dissoc (assoc-in params (cons base keys) v)
                             k))
                   params)))
             params params))

(defn true-values
  [map]
  (u/forf [[k v] map]
    (when (= v "true") (name k))))

(defn dim-type
  [d]
  (get-in dd/dims [(keyword d) :type] :string))

;;; A kludge, we have one boolean field and it needs different handling
(defn dim-boolean?
  [d]
  (= :boolean (dim-type d)))

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
           (cond (empty? vals) "1 = 1"
                 ;; Special case boolean field
                 (dim-boolean? dim)
                 (cond (= vals '("true"))
                       (name dim)
                       (= vals '("false"))
                       (str "not " (name dim))
                       :else "true")
                 :else (format "%s in %s" (name dim) (bq/sql-lit-list vals)))))))))



;;; TODO not sure feature_type hack is working
(defn query1
  [{:keys [feature dim filters feature_type] :as params}]
  (log/info :query1 params)
  (when (and feature dim)
    (select "feature_value, {{dim}} {{from}} 
where feature_variable = '{{feature}}'
AND NOT {{dim}} {{na1}}
AND NOT {{dim}} {{na2}}
{{feature-type-clause}}
AND {{where}}" 
            (assoc params
                   :where (str (joint-where-clause (dissoc filters (keyword feature))))
                   :feature-type-clause (if feature_type (u/expand-template "AND feature_type = '{{feature_type}}'" params) "")
                   :na1 (if (dim-boolean? dim) "IS NULL" "= 'NA'")
                   :na2 (if (dim-boolean? dim) "IS NULL" "= 'Unknown'"))
            )
    ))

;;; Allowable feature values for a single dim, given feature and filters
(defn query1-pop
  [{:keys [dim feature filters] :as params}]
  (when dim
    (->>
     (select "distinct {{dim}} {{from}} where {{feature-clause}} AND {{where}}"
             (assoc params
                    :feature-clause (if feature (u/expand-template "feature_variable = '{{feature}}'" {:feature feature} :key-fn keyword) "1=1")
                    :where (joint-where-clause (dissoc filters (keyword dim)))))
     (map (comp second first))
     set)))

(defn heatmap
  [{:keys [dim filter]}]
  (when dim
    (-> (select "avg(feature_value) as mean, feature_variable, {{dim}} {{from}} 
 where feature_type = 'marker_intensity' and {{where}}
 group by feature_variable, {{dim}}"
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
      (-> (select "avg(feature_value) as mean, feature_variable, {{dim}} {{from}} 
 where feature_variable in {{feature-list}}
 AND NOT {{dim}} {{na1}}
 AND NOT {{dim}} {{na2}}
 and {{where}}
 group by feature_variable, {{dim}}"
                  :feature-list (bq/sql-lit-list feature-list)
                  :dim dim
                  :na1 (if (dim-boolean? dim) "IS NULL" "= 'NA'")
                  :na2 (if (dim-boolean? dim) "IS NULL" "= 'Unknown'")
                  :where (joint-where-clause filter))
          ))))

(u/defn-memoized bio_feature_type-features
  [bio_feature_type]
  (map :feature_variable                ;(comp patch-feature
       (select "distinct feature_variable {{from}} where bio_feature_type = '{{bio_feature_type}}'"
               :bio_feature_type bio_feature_type)))

(u/defn-memoized feature-feature_type-features
  [feature_type]
  (map :feature_variable
       (select "distinct feature_variable {{from}} where feature_type = '{{feature_type}}'"
               :feature_type feature_type)))

(defmethod wd/data :universal
  [params]
  (query1 (params-remap params)))

(defmethod wd/data :populate
  [params]
  (query1-pop (params-remap params)))

(defmethod wd/data :features
  [params]
  (let [{:keys [bio_feature_type feature-feature_type]} (params-remap params)]
    (sort
     (cond bio_feature_type
           (bio_feature_type-features bio_feature_type)
           feature-feature_type
           (feature-feature_type-features feature-feature_type)))))
          
(defmethod wd/data :heatmap
  [params]
  (heatmap (params-remap params)))

(defmethod wd/data :heatmap2
  [params]
  (heatmap2 (params-remap params)))

;;; curl "http://localhost:1199/api/data?data-id=rna-autocomplete&prefix=CL"

;;; Note: in theory, might want to be parameterizable by feature_type = Immune_High or _Low, but in practice these are the same
;;; Value: list of matching strings
(defmethod wd/data :rna-autocomplete
  [params]
  (map :feature_variable
       (select "distinct(feature_variable) {{from}}
where bio_feature_type = 'spatial_RNA'
and feature_variable like '{{prefix}}%%' order by feature_variable limit 20"
               params)))

(u/def-lazy matrix-data
  (mapcat (fn [d]
            (select "Tumor_Diagnosis, {{dim}} as value, '{{dim}}' as dim, count(distinct(sample_id)) as samples
{{from}}
WHERE {{na}}
GROUP BY Tumor_Diagnosis, {{dim}}"
                    {:dim (name d)
                     ;; I believe scientists asked to exclude Unknown/NA values
                     :na (if (dim-boolean? d) "TRUE" "NOT ({{dim}} = 'NA'  OR {{dim}} = 'Unknown')")
                     }))
          (rest (keys dd/dims))))       ;rest because y dim is always :Tumor_Diagnosis

(defmethod wd/data :dist-matrix
  [_]
  @matrix-data)

(defn read-csv-maps
  "Given a tsv file with a header line, returns seq where each elt is a map of field names to strings"
  [f]
  (let [rows (csv/read-csv-file f)]
    (map #(zipmap (map keyword (first rows)) %)
         (rest rows))))

(u/def-lazy vitessce-data
  (read-csv-maps (io/resource "data/20241213_vitesscesamples.csv")))

(defmethod wd/data :vitessce
  [_]
  @vitessce-data)

;;; For Sample page

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
          {:table   metadata-table })
  )

(u/def-lazy feature-variables (map :feature_variable (select "distinct(feature_variable) {{from}} " )))
