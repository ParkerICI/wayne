(ns wayne.bigquery
  (:require [org.candelbio.multitool.core :as u]
            [clojure.data.json :as json]
            #_ [catamite.utils :as cu]
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

#_
(def ds (.getDataset service dataset-id (make-array BigQuery$DatasetOption 0)))


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
  
(comment
  (query project-id "select trim(fuckme, \" []'\"), flow_id, flow_run_id  from ganymede_dev.__FLOW_METADATA, unnest(split(inputs, ',')) as fuckme where inputs LIKE '%sample_2.csv%' limit 10")

  (query project-id "select parsed, inputs, flow_id, flow_run_id from ganymede_dev.__FLOW_METADATA, unnest(json_extract_array(inputs)) as parsed  limit 100")

  (query project-id "ganymede-dev-core" "select *  from ganymede_dev.__FLOW_METADATA where inputs LIKE '%gs:%'")

  )

;;; Think this is wrong. inputs_dict is real JSON, inputs is an array of single-quoted garbage.


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

(defn parse-jank
  [s]
  (map second (re-seq #"'(.*?)'" s)))

(defn flow-metadata
  [env]
  (map (fn [row]
         (-> row
             (assoc :inputs_raw (:inputs row))
             (update :inputs parse-jank) ;This might be entirely redundant with the dict
             (update :inputs_dict #(if (empty? %) nil (json/read-str %)))))
       (query "ganymede-core" (format "select * from %s.__FLOW_METADATA" env))))

(defn output-metadata
  [env]
  (map (fn [row]
         (-> row
             ))
       (query "ganymede-core" (format "select * from %s.__OUTPUT_METADATA" env))))

(defn table-metadata
  [env]
  (map (fn [row]
         (-> row
             (update :table_columns parse-json)
             ))
       (query "ganymede-core" (format "select * from %s.__TABLE_METADATA" env))))

;;; __DATA_METADATA doesn't seem to actually be used

(defn flow-row-files
  [{:keys [inputs_dict inputs] :as flow-row}]
    (filter #(and (string? %) (re-find #"/" %))
            (if inputs_dict
              (flatten (vals inputs_dict))
              (flatten inputs))))

;;; TODO other table?
(defn metadata-files
  [env]
  (->> env
       flow-metadata
       (mapcat flow-row-files)
       distinct))

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

;;; Ex: (add-row "ganymede-core-dev" (table-named (dataset-named "ganymede-core-dev" "ganymede_dev") "mt_timestamp_test") {"name" "foo" "time" "2023-07-10"})
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

(comment
  (def foo (query project-id
                  "select trim(split, \" []'\") as inputx, flow_run_id, flow_id, initiator, initiator_type  from ganymede_dev.__FLOW_METADATA, unnest(split(inputs, ',')) as split limit 100"))
  )



(comment
(defn dev-tables
  [project-id dataset-id]
  (bq/tables (dataset-named project-id dataset-id)))

(defn dev-table
  [named]
  (table-named (dataset-named project-id dataset-id) named))

;;; Find me a table with a boolean column
  (doseq [t (dev-tables)]
    (doseq [f (table-schema-max project-id t)]
      (when (= "BOOLEAN" (str (:type f)))
        (prn :hey t f))))
  )




    


  

