(ns way.vega
  (:require [clojure.walk :as walk]
            [clojure.data.json :as json])
  )

;;; Dev util

(defn include-data
  [spec]
  (walk/postwalk
   (fn [thing]
     (if (and (map-entry? thing) (= :url (first thing)))
       (first {:values (json/read-str (slurp (str "resources/public/" (second thing))))})
       thing))
   spec))

;;; Convert

(defn read-spec-file
  [f]
  (json/read-str (slurp f) :key-fn keyword))


