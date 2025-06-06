(ns org.parkerici.wayne.bigquery
  (:require [org.candelbio.multitool.core :as u]
            [clojure.data.json :as json]
            [clojure.string :as str]
            )
  (:import [com.google.cloud.bigquery BigQuery BigQueryOptions
            TableId
            InsertAllRequest
            Schema Field StandardSQLTypeName StandardTableDefinition TableInfo
            FieldValue
            BigQuery$DatasetListOption
            BigQuery$DatasetOption
            BigQuery$TableListOption
            BigQuery$TableOption
            BigQuery$JobOption
            QueryJobConfiguration
            ])
  )

;;; Local authentication is via ~/.config/gcloud/application_default_credentials.json , set by gcloud cli:
;;;   gcloud auth application-default login
;;; See https://cloud.google.com/docs/authentication/provide-credentials-adc

;;; Unpage gcs results
(defn unpage
  [thing]
  (-> thing
      (.iterateAll)
      (.iterator)
      iterator-seq))

(u/defn-memoized service
  [project]
  (-> (BigQueryOptions/newBuilder)
      (.setProjectId project)
      (.build)
      (.getService)))

(defn datasets
  [project]
  (unpage (.listDatasets (service project)
                         (make-array BigQuery$DatasetListOption 0))))

;;; Not sure how general these are across GCS...maybe elevate

;;; NOT the identifier
;;; eg "Backup demo-0307-h Dataset"
(defn fname
  [thing]
  (.getFriendlyName thing))

;; eg "pici-internal:bruce_external.feature_table"
(defn id
  [thing]
  (.getGeneratedId thing))

(defn tables
  [ds]
  (-> (.list ds (make-array BigQuery$TableListOption 0))
                (.iterateAll)
                (.iterator)
                iterator-seq))

(defn table-schema
  [t]
   (->> t
        .getDefinition
        .getSchema
        .getFields
        (map #(-> %
                  bean
                  u/clean-map
                  (u/fsbl u/map-values str)
                  (dissoc :class)))))

(defn table-schema-max
  [project t]
  (let [bt (.getTable (service project) (.getTableId t) (make-array BigQuery$TableOption 0))]
    (table-schema bt)))

(defn table-name
  [t]
  (->> t
       .getTableId
       .getTable))

(defn dataset-name
  [ds]
  (-> ds
      .getDatasetId
      .getDataset))

(defn list-matching
  [str]
  (filter #(re-matches (re-pattern str)
                       (table-name %))
          tables))

;;; prob better way to do this
(defn table-named
  [ds n]
  (u/some-thing #(= (table-name %) n) (tables ds)))

(defn dataset-named
  [project n]
  (u/some-thing #(= (dataset-name %) n) (datasets project)))

(defn get-value
  [field thing & [repeat]]
  (if (instance? FieldValue thing)
    (cond (and (not repeat) (= "REPEATED" (str (.getMode field))))
          (map #(get-value field % true) (.getValue thing))
          (= "FLOAT" (.name (.getType field))) 
          (.getDoubleValue thing)
          (= "INTEGER" (.name (.getType field))) 
          (.getLongValue thing)
          ;; TODO fill this out
          :else 
          (.getValue thing))
    thing))

;;; Table is provided in select.
(defn query
  [project sql]
  (let [config (.build (QueryJobConfiguration/newBuilder sql))
        results (.query (service project) config (make-array BigQuery$JobOption 0))
        fields (->> results
                    .getSchema
                    .getFields)
        field-names (->> fields
                         (map #(keyword (.getName %))))
        rows (-> results
                 .getValues
                 .iterator
                 iterator-seq)]
    (map (fn [row] (zipmap field-names (map get-value fields row)))
         rows)))

;;; use this for non-query queries like CREATE TABLE
;;; TODO  if result is class com.google.cloud.bigquery.EmptyTableResult 
(defn bare-query
  [project sql]
  (let [config (.build (QueryJobConfiguration/newBuilder sql))
        results (.query (service project) config (make-array BigQuery$JobOption 0))
        ]
    results))

;;; TODO fill this out
(def lookup-type
  {"STRING" StandardSQLTypeName/STRING})

;;; Schema is [[name0 type0]...]
(defn create-table
  [project dataset-name table schema]
  (let [schema (Schema/of (mapv (fn [{:keys [name type]}]
                                  (Field/of name (lookup-type type) (make-array Field 0)))
                                schema))
        table-def (StandardTableDefinition/of schema)
        table-id (TableId/of dataset-name table)
        table-info (.build (TableInfo/newBuilder table-id table-def))]
    (.create (service project) table-info (make-array BigQuery$TableOption 0))))

;;; Ex: (add-row "project" (table-named (dataset-named "project" "dataset") "table") {"name" "foo" "time" "2023-07-10"})
(defn add-row
  [project table row]
  (.insertAll
   (service project)
   (-> (InsertAllRequest/newBuilder (.getTableId table))
       (.addRow "rowId" row)            ;TODO not so sure about that "rowId", maybe a gensym
       (.build))))

(defn clean-row
  [row]
  (->> row
       (u/map-keys name)
       (u/map-values str)))

(defn add-rows
  [project table rows]
  (.insertAll
   (service project)
   (let [builder (InsertAllRequest/newBuilder (.getTableId table))]
     (doseq [row rows]
       (.addRow builder (str (gensym "row")) (clean-row row)))
     (.build builder))))  

(defn sql-lit-list
  [l]
  (str "("
       (str/join ", " (map #(str "'" % "'") l))
       ")"))
