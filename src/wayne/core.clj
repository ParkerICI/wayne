(ns wayne.core
  (:require [wayne.bigquery :as bq]))

(defn -main [& args]
  (prn :hey (bq/datasets "pici-internal")))
