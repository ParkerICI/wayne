(ns org.parkerici.wayne.curate
  (:require [org.parkerici.wayne.data :refer :all]
            [org.candelbio.multitool.core :as u]))

(def select-m (memoize select))

(defn compare-tables
  [t1 t2 index]
  (let [raw1 (select-m "* {{from}}" {:table t1})
        raw2 (select-m "* {{from}}" {:table t2})
        i1 (u/index-by index raw1)
        i2 (u/index-by index raw2)]
    (prn :counts (count raw1) (count raw2))
    (u/map-diff i1 i2)
    ))
