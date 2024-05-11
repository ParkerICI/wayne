
(defn recurrence1
  [{:keys [recurrence final_diagnosis] :as row}]
  (assoc row
         :recurrence1
         (cond (= final_diagnosis "Normal_brain") "Normal_brain"
               (= "no" recurrence) "Primary"
               (= "yes" recurrence) "Recurrence"
               :else "OTHER")))

(comment
(def x2 (select "feature_variable, feature_value, recurrence, final_diagnosis {from} where feature_variable = 'EGFR_func_over_all_tumor_prop'"))
(def x2x (map recurrence1 x2))

(def x3 (map recurrence1 (select (format "feature_variable, feature_value, recurrence, final_diagnosis {from}
 where feature_variable in %s" (wu/sql-lit-list ["EGFR_func_over_all_tumor_prop" "GM2_GD2_func_over_all_tumor_prop"])))))
)

#_
(defn x3p
  [features]
  (map recurrence1 (select (format "feature_variable, feature_value, recurrence, final_diagnosis {from}
 where feature_variable in %s" (wu/sql-lit-list features)))))

(def features1 ["EGFR_func_over_all_tumor_prop"
                "GM2_GD2_func_over_all_tumor_prop"
                "GPC2_func_over_all_tumor_prop"
                "VISTA_func_over_all_tumor_prop"
                "HER2_func_over_all_tumor_prop"
                "B7H3_func_over_all_tumor_prop"
                "NG2_func_over_all_tumor_prop"
                ])


#_
(def x3 (x3p features1))

;;; Note: this is probably wrong, the R code does median per-patient or per-sample or something. But good enough for our purposes I guesss
#_
(def x3a (map (fn [[k v]]
                (assoc (first v)
                       :feature_value
                       (median (map (fn [s] (Double. (:feature_value s))) v))))
              (group-by (juxt :feature_variable :recurrence1) x3)))

#_
(write-json-file "resources/public/hm2.json" x3a)



(def fake-tree-1
  '[[egfr gm2_gd2]
    [[[gpc2 vista]
      her2]
     [b7h3 ng2]]])

(def fake-tree-2
  '[[Primary Recurrence]
    Normal_brain])

(comment
(write-json-file "resources/public/dend1.json" (write-real-tree fake-tree-1))
(write-json-file "resources/public/dend2.json" (write-real-tree fake-tree-2))

(write-real-tree fake-tree-1)
)

#_
(def top20 (map (partial rename-key "" :gene)
                (read-csv-maps "/Users/mt/Downloads/data/RNAseq_mat_top20.csv")))

(comment
(def top20up (-> "/Users/mt/Downloads/data/RNAseq_mat_top20.csv"
                 read-csv-maps
                 (unpivot "" :gene :sample :value)))

(write-json-file "resources/public/sheatmap.json" top20up)
)

(defn write-real-tree
  [fake]
  (let [id-gen (id-generator)]
    (walk-collect
     (fn [x]
       {:id (id-gen x)
        :name (str x)
        :parent (when  (first *side-walk-context*)
                  (id-gen (first *side-walk-context*)))})
     fake)))

#_(write-clusters "resources/public/dend-real-s.json" (cluster top20up :sample :gene :value))
#_(write-clusters "resources/public/dend-real-g.json" (cluster top20up :gene :sample :value))


