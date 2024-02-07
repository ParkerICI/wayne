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
