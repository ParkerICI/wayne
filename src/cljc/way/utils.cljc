(ns way.utils
  (:require [clojure.string :as str])
  )

(defn sql-lit-list
  [l]
  (str "("
       (str/join ", " (map #(str "'" % "'") l))
       ")"))
