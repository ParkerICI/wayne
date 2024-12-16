(ns wayne.csv
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [org.candelbio.multitool.core :as u]
            )
  (:import [org.apache.commons.io.input BOMInputStream]))

;;; Derived from Voracious https://github.com/mtravers/voracious/blob/master/src/voracious/formats/csv.clj
;;; Note: at present, curatorial use only, but eventually will support upload and download

(defn read-csv-file [fname & {:keys [separator quote headers] :as opts}]
  (with-open [reader (-> fname
                         io/input-stream
                         BOMInputStream. ;Removes garbage character
                         io/reader)]
    (doall
     ;; TODO default separator based on filename
     (csv/read-csv reader opts))))
