(ns wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [wayne.frontend.data :as data]
            [wayne.frontend.feature-names :as fn]
            [hyperphor.way.web-utils :as wu]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            )
  )

;;; Complex enough to get its own file

;;; could drive these from a query and API
(def cell-meta-clusters
  ["APC"
   "Bcells"
   "DC_CD123"
   "DC_CD206"
   "DC_Mac_CD209"
   "Endothelial_cells"
   "Immune_unassigned"
   "Macrophage_CD163"
   "Macrophage_CD206"
   "Macrophage_CD68"
   "Mast_cells"
   "Microglia"
   "Myeloid_CD11b"
   "Myeloid_CD14"
   "Myeloid_CD141"
;;; I think this option should not appear?
;   "NA"
   "Neurons"
   "Neutrophils"
   "Tcell_CD4"
   "Tcell_CD8"
   "Tcell_FoxP3"
   "Tumor_cells"
   "Unassigned"])

;;; From Hadeesha TODO figure out how to use

;;; Tumor cell types
(def tumor_func_columns_core_simple
  #{"B7H3_func", "EGFR_func", "GM2_GD2_func" , "GPC2_func" ,  "HER2_func","NG2_func","VISTA_func"})

;;; Immune cell types
(def cell_type_immune 
  #{"Immune_other", "CD68_Myeloid", "CD163_Myeloid",
    "CD8_Tcells", "CD141_Myeloid",
    "Unknown", "CD14_Myeloid",
    "Microglia", "APC", "CD206_Macrophages", "CD4_Tcells",
    "CD11b_Myeloid", "FoxP3_Tcells", "CD123_DC", "Neutrophils",
    "Neurons", "CD209_DC_Mac", "CD208_DC", "Bcells",
    "Mast_Cells"})

;;; Immune functional markers
(def immune_func_marker
  #{"TIM3", "CD38", "Tox", "iNOS", "CD86", "Ki67", "PDL1", "LAG3", "PD1", "ICOS", "IDO1", "GLUT1"}) 




;;; Defines chooses for level 2 and 3
(def non-spatial-features-2-3
  [["marker_intensity" []]                ;special cased
   ;; orange
   ["tumor_cell_features" ["tumor_antigen_co_relative" ; [* "func" * "func" "over" * "func" "prop"]
                           "tumor_antigen_fractions" ; [* "func" "over" * "func" "counts" "plus" * "func_counts_prop"]
                            "tumor_antigen_spatial_density" ; [] ;don't actually fit a pattern? Or do with some optional parts?
                            ]]
   ;; purple
   ["immune_tumor_cell_features" ["immune_cell_relative_to_all_tumor"
                                  "immune_tumor_antigen_fractions"
                                  "immune_cell_functional_relative_to_all_tumor"
                                  "immune_cell_func_tumor_antigen_fractions"]]
   ;; green
   ["immune_cell_features" ["immune_cell_functional_relative_to_all_immune"                            
                            "immune_cell_relative_to_all_immune"
                            "immune_cell_fractions"
                            "immune_functional_marker_fractions"
                            "immune_cell_functional_spatial_density"
                            "immune_cell_spatial_density"                            
                            ]]
   ])

(defn trim-prefix
  [id]
  (subs (name id) (count "feature-")))

(defn row
  [label contents]
  [:div.row.my-2
   [:div.col-3 [:label.small.pt-2 [:b label]]]  ;TEMP for dev I think
   [:div.col-7 contents]])


(defn safe-name
  [thing]
  (when thing
    (or (.-name thing)
        thing)))

;;; TODO nil should be option (could be in values
;;; NOTE used to use :set-param-if which didn't work well, I think that can be removed from Way
(defn select-widget-minimal
  [id values & [extra-action]]
  (let [current-value @(rf/subscribe [:param :features id])]
    (when (and (not (empty? values))
               (not (contains? (set values) current-value)))
      (rf/dispatch [:set-param :features id (safe-name (first values)) ])) 
    (wu/select-widget
     id
     current-value
     #(do
        (rf/dispatch [:set-param :features id %])
        (when extra-action (extra-action %) )) ;ugn
     (map (fn [v]
            {:value v :label (if v (wu/humanize v)  "---")})
          values)
     nil
     nil
     {:display "inherit" :width "inherit" :margin-left "2px"}))) ;TODO tooltips

(defn select-widget
  [id values & [extra-action]]
  [row
   (wu/humanize (trim-prefix id))
   (select-widget-minimal id values extra-action)])



;;; Hack and temporary

(def boilerplate? #{"over" "plus"  "prop" "density"
                    "func"
                    })

(defn resplice
  [[t1 & tail]]
  (cond (nil? t1) '()
        (boilerplate? t1)
        (cons t1 (resplice tail))
        (boilerplate? (first tail))
        (cons t1 (resplice tail))
        :else
        (let [r (resplice tail)]
          (cons (str t1 "_" (first r))
                (rest r)))))

(defn analyze-features
  [f]
  (resplice (re-seq #"[A-Za-z0-9]+" f)))

(defn analyze-feature-class
  [features]
  (let [tokenized (map analyze-features features)]
    (for [i (range (count (first tokenized)))];assuming everything is same size
      (let [tokens (distinct (map #(nth % i) tokenized))]
        tokens))))

(defn subfeature-values
  [feature-type position]
  (-> data/feature-ui-master
      (get (name feature-type))
      first                             ;this gets the first size entry, ideally those will go away
      second
      (nth position)))

(defn feature-template
  [feature-type]
  (-> data/feature-ui-master
      (get (name feature-type))
      first                             ;this gets the first size entry, ideally those will go away
      second
      ))

(def repeated-subfeatures (atom {}))

(defn extend-params
  [feature-type params]
  (let [repeats (get @repeated-subfeatures feature-type)]
    (reduce (fn [m [k v]] (assoc m
                                 (u/keyword-conc feature-type (str k))
                                 (get params (u/keyword-conc feature-type (str v)))
                                 ))
            params
            repeats)))

(defmulti compute-feature-variable
  (fn [feature-type feature-params] feature-type))

(defmethod compute-feature-variable :default
  [feature-type feature-params]
  (let [template (feature-template feature-type)
        ;; join into feature name
        elements
        (map (fn [item i]
               (if (= 1 (count item))
                 (first item)                                             ;boilerplate
                 (get feature-params (u/keyword-conc feature-type (str i))) ;a feature, read out of params
                 ))
             template (range))
        feature (str/join "_" elements)]
    feature))

(defn clean-select-value
  [v]
  (if (= v "---")
    nil
    v))

(rf/reg-sub
 :selected-feature
 (fn [db _]
   ;; stuff into query machinery
   (when (get-in db [:params :features :feature-type])
     (let [feature
           (if (= "marker_intensity" (get-in db [:params :features :feature-type]))
             (get-in db [:params :features :feature-feature])
             (let [feature-type (keyword (get-in db [:params :features :feature-bio-feature-type]))
                   feature-params (get-in db [:params :features])
                   feature-params (u/map-values clean-select-value feature-params)
                   feature-params (extend-params feature-type feature-params)]
               (when (and feature-type feature-params)
                 (compute-feature-variable feature-type feature-params))))] ;methodized
       ;; NOTE Connects up to query machinery. Not completely sure if this is kosher, but it seems to work
       (when-not (= feature (get-in db [:params :universal :feature]))
         (rf/dispatch [:set-param :universal :feature feature]))
       feature))))

(defn feature-valid?
  [feature]
  (contains? fn/feature-names feature))

;;; TODO propagate up into earlier guys
;;; TODO subfeature name not actually used, but could be a tooltip or something
(defn subfeature-selector
  [feature-type subfeature-name template-position & [nullable?]]
  (let [subfeatures (subfeature-values feature-type template-position)
        subfeatures (if nullable? (cons nil subfeatures) subfeatures)
        ;; Encodes the position in the parameter keyword for later extraction. Hacky but simple.
        param-key (u/keyword-conc feature-type (str template-position))] ;  subfeature-name
    (when-not (empty? subfeatures)
      (select-widget-minimal param-key subfeatures))))

(defn subfeature-selector-literal
  [feature-type subfeature-name template-position subfeatures]
  (let [;subfeatures (subfeature-values feature-type template-position)
        param-key (u/keyword-conc feature-type (str template-position))] ;  subfeature-name
    (when-not (empty? subfeatures)
      ;; Encodes the position in the parameter keyword for later extraction. Hacky but simple.
      (select-widget-minimal param-key subfeatures))))

;;; Used for non-boilerplate elements (like repeated antigen names)
(defn boilerplate
  [s]
  [:span.mx-2.pt-2.text-nowrap s])

(defmulti feature-ui keyword)

(defmethod feature-ui :default
  [feature-type]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [:b.sm "defaulted"]
   (map (fn [elt i]
          (if (= 1 (count elt))
            (boilerplate (first elt))
            [subfeature-selector feature-type nil i])) ;sadly no subfeature name but it isn't used for anything anyway
        (feature-template feature-type)
        (range))])

;;; TODO some of these could be handled by default

(defmethod feature-ui :tumor_antigen_co_relative
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :tumor_antigen_co_relative :antigen1 0]
   [subfeature-selector :tumor_antigen_co_relative :antigen2 2]
   [boilerplate "over"]
   (let [antigen1 @(rf/subscribe [:param :features :tumor_antigen_co_relative-0])
         antigen2 @(rf/subscribe [:param :features :tumor_antigen_co_relative-2])] ;TODO this is optional, need to handle that
     ;; TODO all_tumor not in dataset?
     [subfeature-selector-literal :tumor_antigen_co_relative :denominator 5 [antigen1 antigen2 "all_tumor"]])])

;;; TODO. the db variables are insonsistant about naming (the two terms of the denominator can be swapped)

(defn repeater
  [feature-type boss-pos flunky-pos]
  (let [subfeature-param (u/keyword-conc feature-type (str boss-pos))]
    (swap! repeated-subfeatures assoc-in [feature-type flunky-pos] boss-pos)
    (boilerplate (wu/humanize @(rf/subscribe [:param :features subfeature-param])))))            ;TODO needs to get plugged into feature name generator somehow (pos 3))

(defmethod feature-ui :tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :tumor_antigen_fractions :antigen1 0]
   [boilerplate "over"]
   [repeater :tumor_antigen_fractions 0 3]
   [boilerplate "plus"]
   [subfeature-selector :tumor_antigen_fractions :denominator 7]
   ])

(defmethod feature-ui :tumor_antigen_spatial_density
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :tumor_antigen_spatial_density :antigen1 0 true]
   (let [antigen1 @(rf/subscribe [:param :features :tumor_antigen_spatial_density-0])]
     (when antigen1                     
       ;; TODO could exclude antigen1
       [subfeature-selector :tumor_antigen_spatial_density :antigen2 2 true]
       ))])

;;; feature name generation needs to be more complex in this case.
(defmethod compute-feature-variable :tumor_antigen_spatial_density
  [_ feature-params]
  (let [a1 (:tumor_antigen_spatial_density-0 feature-params)
        a2 (:tumor_antigen_spatial_density-2 feature-params)]
    (if (nil? a2)
      (if (nil? a1)
        "all_tumor_count_density"       ;no antigens
        (str a1 "_func_density"))       ;1 antigen
      (str a1 "_func_" a2 "_func_density") ;2 antigens
      ))) 

(defmethod feature-ui :immune_cell_relative_to_all_tumor
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :immune_cell_relative_to_all_tumor :cell_type 0]
   [boilerplate "over all tumor count"]]
  )

(defmethod feature-ui :immune_tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :immune_tumor_antigen_fractions :antigen 0]
   [boilerplate "over"]
   [subfeature-selector :immune_tumor_antigen_fractions :cell_type 3]
   [boilerplate "plus"]
   (repeater :immune_tumor_antigen_fractions 0 5)
   ])


(defmethod feature-ui :immune_cell_functional_relative_to_all_tumor
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_functional_relative_to_all_tumor :cell-type 0)
   (subfeature-selector :immune_cell_functional_relative_to_all_tumor :functional_marker 1)
   (boilerplate "over all tumor count")
   ])

(defmethod feature-ui :immune_cell_func_tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :cell-type 0)
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :functional_marker 1)
   (boilerplate "over")
   (repeater :immune_cell_func_tumor_antigen_fractions 1 3)
   (boilerplate "plus")
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :relative 5)
   ])

(defmethod feature-ui :immune_cell_functional_relative_to_all_immune
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_functional_relative_to_all_immune :cell-type 0) ;needs gluing
   (boilerplate "over all immune count")
   ])

(defmethod feature-ui :immune_cell_relative_to_all_immune
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_relative_to_all_immune :cell-type 0) ;needs gluing
   (boilerplate "over all immune count")
   ])

(defmethod feature-ui :immune_cell_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :immune_cell_fractions :cell-type-1 0]
   [boilerplate "over"]
   [repeater :immune_cell_fractions 0 2]
   [boilerplate "plus"]
   [subfeature-selector :immune_cell_fractions :cell-type-2 4]
   ])

;;; Note: this has the inconsistent plus ordering problem
(defmethod feature-ui :immune_functional_marker_fractions
  [_]
  [:div.border.p-2 {:style {:display "inline-flex"}}
   [subfeature-selector :immune_functional_marker_fractions :cell-type-1 0]
   [subfeature-selector :immune_functional_marker_fractions :functional-marker-1 1]
   [boilerplate "over"]
   [subfeature-selector :immune_functional_marker_fractions :cell-type-2 3]
   [subfeature-selector :immune_functional_marker_fractions :functional-marker-2 4]
   [boilerplate "plus"]
   [repeater :immune_functional_marker_fractions 0 6]
   [repeater :immune_functional_marker_fractions 1 7]
   ])

;;; 2 part version, works but not what was speced
#_
(defmethod feature-ui :immune_functional_marker_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :immune_functional_marker_fractions :combo-1 0]
   [boilerplate "over"]
   (repeater :immune_functional_marker_fractions 0 2)
   [boilerplate "plus"]
   [subfeature-selector :immune_functional_marker_fractions :combo-2 4]
   ])

;;; TODO this is not right, should filter features by meta-cluster I think?
(defn l3-feature
  []
  (select-widget :feature-feature data/features #(rf/dispatch [:set-param :universal :feature %])))

(defn l2-spatial
  []
  [:i "TBD"])

;;; → Multitool?
(defn conjs
  [coll thing]
  (if (nil? coll)
    #{thing}
    (conj coll thing)))

(defn l2-nonspatial
  []
  [:div 
   (select-widget :feature-type (map first non-spatial-features-2-3))
   (let [feature-l2 @(rf/subscribe [:param :features :feature-type])]
     (if (= "marker_intensity" feature-l2)
       [:div                            ;TODO wire this into feature output
        (select-widget :feature-meta-cluster cell-meta-clusters)
        [l3-feature]]
       ;; Not marker_intensity
       [:div
        (select-widget
         :feature-bio-feature-type
         (get (into {} non-spatial-features-2-3) feature-l2)
         #(rf/dispatch [:set-param :heatmap :bio_feature_type %])
         )
        (when-let [bio_feature_type @(rf/subscribe [:param :features :feature-bio-feature-type])]
           [row "feature" [feature-ui bio_feature_type]] )]))
   ])

(defn feature-list-ui
  []
  (let [feature @(rf/subscribe [:selected-feature])
        feature-list @(rf/subscribe [:param :heatmap2 :feature-list])]
    [:div
     [row "feature_variable"
      [:span
       (wu/humanize feature)
       [:b (str " " (if (feature-valid? feature) "present " "ND") )] ;TODO EnJun wants to wordsmith these
       (when (and (feature-valid? feature)
                  (not (contains? feature-list feature)))
         [:a #_ :button.btn.btn-sm.btn-secondary.mx-2
          {:href "#"
           :on-click #(rf/dispatch [:param-update :heatmap2 :feature-list conjs feature])}
          "add"])
       ]]
     ;; TODO lozenge UI
     [row
      [:span "feature list "
       (when-not (empty? feature-list)
         [:a {:href "#" :on-click #(rf/dispatch [:set-param :heatmap2 :feature-list #{}])} "clear"])]
      (str/join ", " (map wu/humanize feature-list))]]))



;;; → way/params
(rf/reg-event-db
 :param-update
 (fn [db [_ data-id param f & args]]
   (let [v (get-in db [:params data-id param])]
     (hyperphor.way.params/set-param db [:foo data-id param (apply f v args)]))))

(defn ui
  []
  [:div
   [:div.row
    [:div.col-10
     [select-widget :feature-supertype ["non-spatial" "spatial"]]
     (if (= "non-spatial" @(rf/subscribe [:param :features :feature-supertype]))
       [l2-nonspatial]
       [l2-spatial])
     ]]
   ])

