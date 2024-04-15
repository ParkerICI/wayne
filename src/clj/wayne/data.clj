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
                    {:from " FROM `pici-internal.bruce_external.feature_table_20240409` "}
                    #_ {:from " FROM `pici-internal.bruce_external.feature_table_0307` "})
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
               (if (= v "null")         ;fix "null"s
                 (dissoc params k)      
                 (if (and (string? k) (re-find #"\[.*\]" k)) 
                   (dissoc (assoc-in params (mapv keyword (re-seq #"\w+" k)) v) k) ;TODO a bit hacky
                   params)))
             params params))

;;; Repeated in universal.cljs
(def grouping-features [:final_diagnosis :who_grade :ROI :recurrence
                        :treatment :idh_status])
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
;;; TODO hopefull obso
;;; Yes there is no need for this specifiuc thing, wasn't there something that
;;; got POPULATED values????
;;; Not actively used
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
  [{:keys [feature dim filters]}]
  (when (and feature dim)
    (-> (select "feature_value, {dim} {from} 
where feature_variable = '{feature}' AND {where}"
                :dim dim
                :feature feature
                :where (joint-where-clause filters))
        clean-data)))

;;; Allowable feature values for a single dim, given feature and filters
;;; TODO should delete dim from filters, here or upstream
(defn query1-pop2
  [{:keys [dim feature filters] :as params}]
  (when dim
    (->>
     (select "distinct {dim} {from} where {feature-clause} AND {where}"
             (assoc params
                    :feature-clause (if feature (u/expand-template "feature_variable = '{feature}'" {:feature feature} :key-fn keyword) "1=1")
                    :where (joint-where-clause (dissoc filters (keyword dim)))))
     (map (comp second first))
     set)))

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
  (-> (case (if (vector? data-id) (first data-id) data-id) ;TODO multimethod or some other less kludgerous form
        "patients" (patient-table)
        "sites" (site-table)
        "samples" (sample-table)
        "dotplot" (data0 params)
        "barchart" (data0 params)
        "violin" (data0 params)
        "universal" (query1 (params-remap params))
        "universal-pop" (query1-pop2 (params-remap params))
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

;;; Generate a map of filter dims and values, pasted by hand into front end for now
(defn generate-filters
  []
  (zipmap grouping-features
          (map #(sort (map (comp second first) (query1-meta {:feature % :filters {}})))
               grouping-features)))


;;; For filter-feature table

;;; The new set
(def filter-features
  '{:ROI
    ("INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR" "SOLID_TUMOR" "TUMOR" "other"),
    :treatment
    ("CHOP_brain_cptac_2020"
     "CHOP_openpbta"
     "CHOP_pbta_all"
     "CHOP_unknown"
     "CoH_control"
     "CoH_neoadjuvant"
     "Stanford_unknown"
     "UCLA_control"
     "UCLA_neoadjuvant_nonresp"
     "UCLA_neoadjuvant_resp"
     "UCSF_0"
     "UCSF_lys_control"
     "UCSF_neoadjuvant_SPORE_CD27"
     "UCSF_neoadjuvant_SPORE_vaccine"
     "UCSF_neoadjuvant_lys_vaccine"
     "UCSF_non_trial_controls"
     "UCSF_pre_trial"
     "UCSF_pxa_group"),
    :who_grade ("2" "3" "4" "NA"),
    :idh_status ("NA" "mutant" "unknown" "wild_type"),
    :recurrence ("no" "unknown" "yes"),
,
    :final_diagnosis
    ("Astrocytoma"
     "Diffuse_midline_glioma"
     "GBM"
     "Ganglioglioma"
     "Glioma"
     "Normal_brain"
     "Oligodendroglioma"
     "PXA"
     "Thalmic_glioma"
     "pGBM"
     "pHGG")})

;;; → Multitool!
(defn replace-key
  [map old new]
  (-> map
      (assoc new (get map old))
      (dissoc old)))

(defn grid
  []
  (u/forcat [dim1 grouping-features
             dim2 grouping-features]
    (map (fn [row]
           (-> row
               (update :dim1 #(str (name dim1) ": " %))
               (update :dim2 #(str (name dim2) ": " %))
               ))
         (select (format "%s as dim1, %s as dim2, count(distinct(patient_id)) as count {from} group by %s, %s" (name dim1) (name dim2)(name dim1) (name dim2))))))
              
#_
(write-json-file "resources/public/filter-grid.js" (grid))

;;; Feature hacking


(comment
(def features (select "distinct feature_variable, feature_type, cell_meta_cluster_final, feature_source, bio_feature_type, Feature {from} "))

(map #(frequencies (map % features)) [:feature_type, :cell_meta_cluster_final, :feature_source, :bio_feature_type, :Feature])
)
