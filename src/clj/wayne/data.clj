(ns wayne.data
  (:require [wayne.bigquery :as bq]
            [org.candelbio.multitool.core :as u]))

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
  (map :site (bq/query "pici-internal" "select distinct site from `pici-internal.bruce_external.feature_table` ")))

(defn feature-types
  []
  (map :feature_type (bq/query "pici-internal" "select distinct feature_type from `pici-internal.bruce_external.feature_table` ")))

(def sites '("CoH" "CHOP" "UCLA" "UCSF" "Stanford"))

;;; 14K features! Kind of useless
(defn features
  [site type]
  (map :feature_variable
       (bq/query "pici-internal"
                 (format "select distinct feature_variable from `pici-internal.bruce_external.feature_table` where site = '%s' and feature_type = '%s'" site type))))

(defn features+
  [site]
  (bq/query "pici-internal" (format  "select distinct feature_variable, feature_type  from `pici-internal.bruce_external.feature_table` where site = '%s'" site))))

(defn clean-data
  [d]
  (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v)))))
       d))

(defn data0
  [params]
  (let [query (query0 params)]
    (prn :data params query)
    (->> (bq/query "pici-internal" query)
         clean-data)))

#_(api {:site "Stanford" :feature "CD86"})

;;; curl "http://localhost:1088/api/v2/data0?site=Stanford&feature=CD86"
