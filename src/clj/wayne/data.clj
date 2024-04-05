(ns wayne.data
  (:require [way.bigquery :as bq]
            [way.utils :as wu]
            [way.debug :as debug]
            [taoensso.timbre :as log]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.math :as mu]
            [clojure.string :as str]))

(defn query
  [q]
  (bq/query "pici-internal" q))

(defn select
  [q & {:keys [] :as args}]
  (bq/query "pici-internal"
            (u/expand-template
             (str "select " q)
             (merge args
                    {:from " FROM `pici-internal.bruce_external.feature_table_0307` "})
             :key-fn keyword)))



;;; includes samples and aggregates into vector when necessary
;;; Note: this is a big pain, might want to generate it from a schema
(defn patient-table
  []
  (select "patient_id,
ANY_VALUE(site) as site, 
ANY_VALUE(`group`) as `group`, 
ANY_VALUE(cohort) as cohort, 
ANY_VALUE(immunotherapy) as immunotherapy, 
array_agg(distinct(who_grade)) as who_grade, 
array_agg(distinct(final_diagnosis)) as final_diagnosis, 
ANY_VALUE(recurrence) as recurrence, 
ANY_VALUE(progression) as progression, 
array_agg(distinct(sample_id)) as samples,
array_agg(distinct(fov)) as fovs
{from}
group by patient_id"))

;;; Sites

(defn site-table
  []
  (select "site,
count(distinct(patient_id)) as patients,
count(distinct(sample_id)) as samples,
count(distinct(feature_variable)) as features
{from} group by site"))


;;; Cohorts

(comment 
  (query "select cohort, count(distinct(patient_id)) as patients, count(distinct(sample_id)) as samples from `pici-internal.bruce_external.feature_table` group by cohort")
  )

;;;; Samples
(defn sample-table
  []
  (select "sample_id, 
any_value(patient_id) as patient_id, 
any_value(fov) as fov, 
count(1) as values, 
count(distinct(feature_variable)) as features
{from}
group by sample_id"))


(defn clean-data
  [d]
  (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v)))))
       d))

#_
(defn query0
  [{:keys [site feature]}]
  (select "ROI, immunotherapy, feature_value {from} 
where 
site = '{site}' 
and final_diagnosis = 'GBM' 
and feature_variable = '{feature}' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
" :site site :feature feature))


;;; This currently handles data for both Violin and Scatter panes

(defn query0
  [{:keys [site feature rois]}]
  (select "site, ROI, immunotherapy, feature_value, patient_id, sample_id, cell_meta_cluster_final, fov, feature_variable {from} 
where 
1 = 1
{site}
{feature}
{rois}
"
          :site (when site (format "and site = '%s'" site))
          :feature (when feature (format "and feature_variable = '%s' " feature))
          :rois (when rois (str "and ROI IN " (wu/sql-lit-list rois)))))

;;; General trick to convert maps in get params back into their real form.
;;; → Way
(defn params-remap
  [params]
  (reduce-kv (fn [params k v]
               (if (and (string? k) (re-find #"\[.*\]" k)) 
                 (dissoc (assoc-in params (mapv keyword (re-seq #"\w+" k)) v) k) ;TODO a bit hacky
                 params))
             params params))

;;; Repeated in universal.cljs
(def grouping-vars [:final_diagnosis :who_grade :cohort :ROI :recurrence])



(defn true-values
  [map]
  (u/forf [[k v] map]
    (when (= v "true") (name k))))

(defn joint-where-clause
  [values-map]
  (str/join
   " AND "
   (cons "1 = 1"
         (when values-map
           (for [dim (keys values-map)]
             (let [vals (true-values (get values-map dim))]
               (if (empty? vals)
                 "1 = 1"
                 (format "%s in %s" (name dim) (wu/sql-lit-list vals)))))))))


;;; TODO feature is misnomer, change to dim or something
(defn query1-meta
  [{:keys [feature filters] :as params}]
  ;; TODO conditionalize to avoid prod errors
  #_ (debug/view :query1-meta params)
  (when feature
    (select "distinct {dim} {from} 
where {where}
"
            :dim (name feature)
            :where (joint-where-clause (dissoc filters (keyword feature))))
    ))

(defn query1
  [{:keys [feature dim filter]}]
  #_ (way.debug/view :query1 params)
  ;; TODO adding site for heatmap, temp
  (when (and feature dim)
    (-> (select "feature_value, {dim}, site {from} 
where feature_variable = '{feature}' AND {where}"
                :dim dim
                :feature feature
                :where (joint-where-clause filter))
        clean-data)))

(defn data0
  [params]
  (-> (query0 params)
      clean-data))

(defn heatmap
  [{:keys [dim filter]}]
  (when dim
    (-> (select "avg(feature_value) as mean, feature_variable, {dim} {from} 
 where feature_type = 'intensity'
 group by feature_variable, {dim}"
                :dim dim
                :where (joint-where-clause filter))
        )))

(defn denil
  [thing]
  (if (nil? thing) [] thing))

(defn data
  [{:keys [data-id] :as params}]
  (log/info :data params)
  (-> (case data-id                     ;TODO multimethod or some other less kludgerous form
        "patients" (patient-table)
        "sites" (site-table)
        "samples" (sample-table)
        "dotplot" (data0 params)
        "barchart" (data0 params)
        "violin" (data0 params)
        "universal-meta" (query1-meta (params-remap params)) ; 
        "universal" (query1 (params-remap params))
        "heatmap" (heatmap (params-remap params))
        )
      denil))                           ;TODO temp because nil is being used to mean no value on front-end...lazy

;;; Some data exploration tools, they are general so move to WAY

;;; TODO version that includes frequences
;;; TODO do these dynamically, incorporating constraints

(defn values
  [field]
  (mapv (keyword field) (select (format "distinct `%s` {from}" field))))

(defn field-frequencies
  [field]
  (let [r (select (format "count(1) as count, %s as value {from} group by %s" field field))]
    (zipmap (map :value r) (map :count r))))


(defn cols
  []
  (keys (first (select "* {from} limit 1"))))

(defn too-big
  [limit seq]
  (if (> (count seq) limit)
    (count seq)
    seq))

(defn all-values
  [limit]
  (let [fields (cols)]
    (zipmap fields
            (map (comp (partial too-big limit) values name) fields))))

;;; (all-values 100)
(def values-c
  {:patient_id 268,
   :group ["unknown" "A" "B" "C" "D"],
   :ROI
   ["other" "TUMOR" "SOLID_TUMOR" "INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR"],
   :site ["CoH" "CHOP" "UCLA" "UCSF" "Stanford"],
   :immunotherapy ["false" "true"],
   :feature_type
   ["intensity"
    "tumor_cell_ratios"
    "immune_cell_ratios"
    "immune_func_ratios"
    "tumor_cell_density"
    "immune_cell_density"
    "immune_func_density"
    "immune_func_ratios_to_all"],
   :feature_value 350000,
   :cell_meta_cluster_final
   ["Macrophage_CD68"
    "Unassigned"
    "Myeloid_CD14"
    "Tcell_CD8"
    "Neurons"
    "Tumor_cells"
    "Tcell_FoxP3"
    "Microglia"
    "APC"
    "Neutrophils"
    "Endothelial_cells"
    "Myeloid_CD141"
    "Tcell_CD4"
    "Macrophage_CD163"
    "Immune_nassigned"
    "Immune_unassigned"
    "Macrophage_CD206"
    "DC_CD123"
    "Myeloid_CD11b"
    "DC_Mac_CD209"
    "Mast_Cells"
    "DC_CD206"
    "Bcells"
    "NA"],
   :who_grade ["4" "unknown" "2" "3"],
   :fov 603,
   :sample_id 590,
   :int64_field_0 350000,
   :feature_source ["cell_meta_cluster_final" "whole_sample"],
   :recurrence ["yes" "unknown" "no"],
   :cohort
   ["neoadjuvant"
    "control"
    "unknown"
    "pbta_all"
    "neoadjuvant_resp"
    "pre_trial"
    "0"
    "neoadjuvant_lys_vaccine"
    "openpbta"
    "brain_cptac_2020"
    "neoadjuvant_nonresp"
    "lys_control"
    "neoadjuvant_SPORE_CD27"
    "neoadjuvant_SPORE_vaccine"
    "non_trial_controls"
    "pxa_group"],
   :final_diagnosis
   ["GBM"
    "Astrocytoma"
    "PXA"
    "Oligodendroglioma"
    "Normal_brain"
    "pGBM"
    "Thalmic_glioma"
    "Glioma"
    "pHGG"
    "Diffuse_midline_glioma"
    "Ganglioglioma"],
   :feature_variable 14140,
   :progression ["unknown" "no" "yes"]})


;;; based on 0307 feature data

(def values-d
  {:patient_id 268,
   :group ["unknown" "A" "B" "C" "D"],
   :ROI
   ["other" "TUMOR" "SOLID_TUMOR" "INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR"],
   :site ["CoH" "CHOP" "UCLA" "UCSF" "Stanford"],
   :immunotherapy ["true" "false"],
   :feature_type
   ["intensity"
    "tumor_cell_ratios"
    "immune_cell_ratios"
    "immune_func_ratios"
    "tumor_cell_density"
    "immune_cell_density"
    "immune_func_density"
    "immune_tumor_cell_ratios"
    "immune_func_ratios_to_all"],
   :source_table ["cell_table_immune" "cell_table_tumor" "NA"],
   :feature_value 218965,
   :cell_meta_cluster_final
   ["Endothelial_cells"
    "Myeloid_CD14"
    "Tumor_cells"
    "APC"
    "Neurons"
    "Macrophage_CD163"
    "Immune_unassigned"
    "Tcell_CD8"
    "Neutrophils"
    "Macrophage_CD68"
    "Microglia"
    "Unassigned"
    "Macrophage_CD206"
    "Myeloid_CD11b"
    "DC_CD123"
    "Myeloid_CD141"
    "Tcell_FoxP3"
    "DC_Mac_CD209"
    "DC_CD206"
    "Bcells"
    "NA"
    "Tcell_CD4"
    "Mast_cells"],
   :treatment
   ["CoH_neoadjuvant"
    "CoH_control"
    "CHOP_unknown"
    "CHOP_pbta_all"
    "UCLA_control"
    "UCLA_neoadjuvant_resp"
    "UCSF_pre_trial"
    "UCSF_0"
    "UCSF_neoadjuvant_lys_vaccine"
    "CHOP_openpbta"
    "CHOP_brain_cptac_2020"
    "UCLA_neoadjuvant_nonresp"
    "UCSF_lys_control"
    "UCSF_neoadjuvant_SPORE_CD27"
    "UCSF_neoadjuvant_SPORE_vaccine"
    "UCSF_non_trial_controls"
    "UCSF_pxa_group"
    "Stanford_unknown"],
   :who_grade ["4" "NA" "2" "3"],
   :fov 604,
   :sample_id 590,
   :idh_status ["wild_type" "unknown" "mutant" "NA"],
   :int64_field_0 350000,
   :feature_source ["cell_meta_cluster_final" "whole_sample"],
   :recurrence ["yes" "unknown" "no"],
   :cohort
   ["neoadjuvant"
    "control"
    "unknown"
    "pbta_all"
    "neoadjuvant_resp"
    "pre_trial"
    "0"
    "neoadjuvant_lys_vaccine"
    "openpbta"
    "brain_cptac_2020"
    "neoadjuvant_nonresp"
    "lys_control"
    "neoadjuvant_SPORE_CD27"
    "neoadjuvant_SPORE_vaccine"
    "non_trial_controls"
    "pxa_group"],
   :feature ["non_spatial"],
   :final_diagnosis
   ["GBM"
    "Astrocytoma"
    "PXA"
    "Oligodendroglioma"
    "Normal_brain"
    "pGBM"
    "Thalmic_glioma"
    "Glioma"
    "pHGG"
    "Diffuse_midline_glioma"
    "Ganglioglioma"],
   :feature_variable 18582,
   :progression ["unknown" "no" "no_later_event" "yes_later_event" "yes"]})

;;; Deconstruct their graph

;;; Features?

#_
(def features (select "distinct feature_type, feature_source, feature_variable {from}"))

;;; Sketches for real heatmap



(comment
  (def xav (select "avg(feature_value) as mean_value, feature_variable, sample_id {from} where feature_type = 'intensity' group by feature_variable, sample_id"))
  (count (distinct (map :feature_variable xav)))
  50
  (count (distinct (map :sample_id xav)))
  589)

;;; TODO generalize, add to voracious or something. 
(defn write-matrix
  [xav]
  (let [cols (distinct (map :feature_variable xav))
        rows (distinct (map :sample_id xav))
        data (u/index-by (juxt :sample_id :feature_variable) xav)]
    (cons
     (cons "sample_id" cols)            ;header row
     (for [row rows]
       (cons row
             (map (fn [col] (get-in data [[row col] :mean_value])) cols))))))

#_
(ju/write-tsv-rows "data/heatmap.tsv" (write-matrix ))

;;; I read this into R and messed with it, see /opt/client/pici/bruce/r-heatmap-transcript
;;; bruce <- read.table("/opt/mt/repos/pici/wayne/data/heatmap.tsv", header = T)





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
