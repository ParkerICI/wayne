(ns way.utils
  (:require [clojure.string :as str])
  )

;;; TODO probably wants to be a sql hacking library
(defn sql-lit-list
  [l]
  (str "("
       (str/join ", " (map #(str "'" % "'") l))
       ")"))
