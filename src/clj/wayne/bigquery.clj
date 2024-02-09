(ns wayne.bigquery
  (:require [org.candelbio.multitool.core :as u]
            [clojure.data.json :as json]
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

;;; Authentication is via ~/.config/gcloud/application_default_credentials.json , set by gcloud cli:
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
  (unpage (.listDatasets (service project)  (make-array BigQuery$DatasetListOption 0))))

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
                  ((u/swapped u/map-values) str)
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


;;; Do a query

;; (defn query
;;   [table sql]
;;   (let [config (.build (QueryJobConfiguration/newBuilder sql))
;;         job-id (.build)


(defn get-value
  [thing]
  (cond (and (seqable? thing) (not (string? thing)))
        (map get-value thing)
        (instance? FieldValue thing)
        (get-value (.getValue thing))
        :else thing))

;;; Nevermind, simpler! Table is provided in select.
(defn query
  [project sql]
  (let [config (.build (QueryJobConfiguration/newBuilder sql))
        results (.query (service project) config (make-array BigQuery$JobOption 0))
        fields (->> results
                    .getSchema
                    .getFields
                    (map #(keyword (.getName %))))
        rows (-> results
                 .getValues
                 .iterator
                 iterator-seq)]
    (map (fn [row] (zipmap fields (get-value row))) rows)))

;;; use this for non-query queries like CREATE TABLE
;;; TODO  if result is class com.google.cloud.bigquery.EmptyTableResult 
(defn bare-query
  [project sql]
  (let [config (.build (QueryJobConfiguration/newBuilder sql))
        results (.query (service project) config (make-array BigQuery$JobOption 0))
        ]
    results))
  
(defn parse-json
  [s]
  (when s
    (try
      (-> s
          (clojure.string/replace \' \")    ;Is JSON really so ill-defined? NO
          json/read-str)
      (catch Throwable e
        (prn :bad-json s e)
        nil))))

(def lookup-type
  {"STRING" StandardSQLTypeName/STRING})

;;; Schema is [[name0 type0]...]

(defn create-table
  [project dataset-name table schema]
  (prn :y schema)
  (let [schema (Schema/of (mapv (fn [{:keys [name type]}]
                                  ;; TODO type
                                  (Field/of name (lookup-type type) (make-array Field 0)))
                                schema))
        table-def (StandardTableDefinition/of schema)
        _ (prn :a table-def)
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






    


  

