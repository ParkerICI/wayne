(ns traverse.heatmap-data
  (:require [org.candelbio.multitool.cljcore :as ju]
            [clojure.string :as str]))

;;; Not loaded in running system

;;; Heatmaps again. Gen data for .ai illustrations

(def x2 (select "feature_variable, feature_value, recurrence, final_diagnosis {from} where feature_variable = 'EGFR_func_over_all_tumor_prop'"))
(defn recurrence1
  [{:keys [recurrence final_diagnosis] :as row}]
  (assoc row
         :recurrence1
         (cond (= final_diagnosis "Normal_brain") "Normal_brain"
               (= "no" recurrence) "Primary"
               (= "yes" recurrence) "Recurrence"
               :else "OTHER")))

(def x2x (map recurrence1 x2))

(def x3 (map recurrence1 (select (format "feature_variable, feature_value, recurrence, final_diagnosis {from}
 where feature_variable in %s" (wu/sql-lit-list ["EGFR_func_over_all_tumor_prop" "GM2_GD2_func_over_all_tumor_prop"])))))


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

(def x3 (x3p features1))

(defn write-json-file [f content]
  (with-open [s (clojure.java.io/writer f)]
    (clojure.data.json/write content s)))

;;; TODO in multitool
(defn median
  [seq]
  (let [sorted (sort seq)
        count (count seq)]
    (if (even? count)
      (mu/mean [(nth sorted (/ count 2))
               (nth sorted (dec (/ count 2)))])
      (nth sorted (/ (dec count) 2)))))
    

;;; Note: this is probably wrong, the R code does median per-patient or per-sample or something. But good enough for our purposes I guesss
(def x3a (map (fn [[k v]]
                (assoc (first v)
                       :feature_value
                       (median (map (fn [s] (Double. (:feature_value s))) v))))
              (group-by (juxt :feature_variable :recurrence1) x3)))

(write-json-file "resources/public/hm2.json" x3a)


(def fake-tree-1
  '[[egfr gm2_gd2]
    [[[gpc2 vista]
      her2]
     [b7h3 ng2]]])

(def fake-tree-2
  '[[Primary Recurrence]
    Normal_brain])

(def ^:dynamic *side-walk-context* ())

(defn side-walk
  "Walks form, an arbitrary data structure, evaluating f on each element for side effects. Note: has nothing to do with the standard (functional) walker, and maybe should have a different name (traverse?)"
  [f form]
  (do 
    (f form)
    (binding [*side-walk-context* (cons form *side-walk-context*)]
      (cond
        (coll? form) (doseq [elt form] (side-walk f elt))
        (map-entry? form)
        (do (side-walk f (key form))
            (side-walk f (val form)))))))

(defn walk-reduce
  "Walks form with an accumulator. f is a function of [accumulator elt], init is initial val of accumulator."
  [f form init]
  (let [acc (atom init)]          ;typically acc should be transient, but since they need special mutators can't be done in a general way. See walk-collect below
    (side-walk
     (fn [elt]
       (swap! acc f elt))
     form)
    @acc))

(defn walk-collect
  "Walk f over thing and return a list of the non-nil returned values"
  [f thing]
  (persistent!
   (walk-reduce (fn [acc elt]
                 (if-let [it (f elt)]
                   (conj! acc it)
                   acc))
               thing
               (transient []))))

(defn id-generator
  []
  (let [next (atom 0)                   ;I suppose should be just one atom
        cache (atom {})]
    (fn [thing]
      (or (get @cache thing)
          (do
            (let [id (swap! next inc)]
              (swap! cache assoc thing id)
              id))))))

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
  
    
(write-json-file "resources/public/dend1.json" (write-real-tree fake-tree-1))
(write-json-file "resources/public/dend2.json" (write-real-tree fake-tree-2))

(write-real-tree fake-tree-1)


;;; Replication baybe

(defn read-csv-rows
  "Read a tsv file into vectors"
  [f]
  (map #(str/split % #",")
       (ju/file-lines f)))

(defn read-csv-maps
  "Given a tsv file with a header line, returns seq where each elt is a map of field names to strings"
  [f]
  (let [rows (read-csv-rows f)]
    (map #(zipmap (first rows) %)
         (rest rows))))

(defn rename-key
  [old new map]
  (-> map
      (assoc new (get map old))
      (dissoc old)))


(def top20 (map (partial rename-key "" :gene)
                (read-csv-maps "/Users/mt/Downloads/data/RNAseq_mat_top20.csv")))

;;; Why am I doing this
(defn unpivot
  [rows idcol idcol2 colcol valcol]
  (mapcat (fn [row]
            (map (fn [[k v]] {idcol2 (get row idcol)
                              colcol k
                              valcol v})
                 (dissoc row idcol)))
          rows))

(def top20up (-> "/Users/mt/Downloads/data/RNAseq_mat_top20.csv"
                 read-csv-maps
                 (unpivot "" :gene :sample :value)))

(write-json-file "resources/public/sheatmap.json" top20up)

;;; OK stupid representation, use a matrix
(defn distance
  [maps idcol1 idcol2 colcol valcal]
  )
