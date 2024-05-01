(ns wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [wayne.frontend.data :as data]
            [way.web-utils :as wu]
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

(defn select-widget
  [id values & [extra-action]]
  (when-not (empty? values)
    (rf/dispatch [:set-param-if :features id (name (first values))])) ;TODO smell? But need to initialize somewhere
  [:div.row
   [:div.col-4  [:label.small.pt-2 [:b (wu/humanize (trim-prefix id))]]]  ;TEMP for dev I think
   [:div.col-6
    (wu/select-widget
     id
     @(rf/subscribe [:param :features id])
     #(do
        (rf/dispatch [:set-param :features id %])
        (when extra-action (extra-action %) )) ;TODO ugh
     (map (fn [v] {:value v :label (wu/humanize v)}) values)
     nil)]])

(defn select-widget-minimal
  [id values]
  (when (and (not (empty? values)) (first values))
    (rf/dispatch [:set-param-if :features id (name (first values))])) ;TODO smell? But need to initialize somewhere
  (wu/select-widget
   id
   @(rf/subscribe [:param :features id])
   #(rf/dispatch [:set-param :features id %]) ;Necessary to allow changes! But also does some kind of gross invalidation
   (map (fn [v] {:value v :label (wu/humanize v)}) values)
   nil
   nil
   {:display "inherit" :width "inherit" :margin-left "2px"}))

;;; TODO this is not right, should filter features by meta-cluster I think?
(defn l3-feature
  []
  (select-widget :feature-feature data/features #(rf/dispatch [:set-param :universal :feature %])))

(defn l2-spatial
  []
  [:i "TBD"])

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

(defn segmented-selector
  [features]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (u/for* [part (analyze-feature-class features)
         i (range)]
     (if (= 1 (count part))
       [:span.mx-2.pt-2 (wu/humanize (first part))]   ;TODO note approximating alignment, not sure how to do it right
       [select-widget-minimal (keyword (str "feature-seg-" i)) part]))])

(defmulti feature-ui keyword)

(defmethod feature-ui :default
  [_]
  [:span "TBD"])

(defn subfeature-values
  [feature-type position]
  (-> data/feature-ui-master
      (get (name feature-type))
      first                             ;this gets the first size entry, ideally those will go away
      second
      (nth position)))

(defmethod feature-ui :tumor_antigen_co_relative
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [select-widget-minimal :tumor_antigen_co_relative_antigen1 (subfeature-values :tumor_antigen_co_relative 0)]
   [select-widget-minimal :tumor_antigen_co_relative_antigen2 (subfeature-values :tumor_antigen_co_relative 2)]
   [:span.mx-2.pt-2 "over"]
   (let [antigen1 @(rf/subscribe [:param :features :tumor_antigen_co_relative_antigen1])
         antigen2 @(rf/subscribe [:param :features :tumor_antigen_co_relative_antigen2])] ;TODO this is optional, need to handle that
     [select-widget-minimal :tumor_antigen_denominator [antigen1 antigen2 "all_tumor"]])])

;;; Note. the db variables are insonsistant about naming (the two terms of the denominator can be swapped)
(defmethod feature-ui :tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [select-widget-minimal :tumor_antigen_fractions_antigen1 (subfeature-values :tumor_antigen_fractions 0)]
   (let [antigen1 @(rf/subscribe [:param :features :tumor_antigen_fractions_antigen1])]
     [:span.mx-2.pt-2 {:style { :display "inline-flex"}}
      [:span.mx-2.pt-2 "over"]
      [select-widget-minimal :tumor_antigen_fractions-denominator (subfeature-values :tumor_antigen_fractions 0)]
      [:span.mx-2.pt-2 "plus"]
      [:span.mx-2.pt-2 antigen1]])])

;; ;TODO null value stuff only partly working
(defmethod feature-ui :tumor_antigen_spatial_density
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [select-widget-minimal :tumor_antigen_spatial_density_antigen1 (cons nil (subfeature-values :tumor_antigen_spatial_density 0))]
   (let [antigen1 @(rf/subscribe [:param :features :tumor_antigen_spatial_density_antigen1])]
     (when antigen1                     
       ;; TODO could exclude antigen1
       [select-widget-minimal :tumor_antigen_spatial_density_antigen2 (cons nil (subfeature-values :tumor_antigen_fractions 0))]
       ))])

(defmethod feature-ui :immune_cell_relative_to_all_tumor
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [select-widget-minimal :immune_cell_relative_to_all_tumor_cell_type (subfeature-values :immune_cell_relative_to_all_tumor 0)]
   [:span.mx-2.pt-2 "over all tumor count"]]
  )

(defmethod feature-ui :immune_tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [select-widget-minimal :immune_tumor_antigen_fractions_antigen (subfeature-values :immune_tumor_antigen_fractions 1)]
   [:span.mx-2.pt-2 "over"]
   [select-widget-minimal :immune_tumor_antigen_fractions_cell_type (subfeature-values :immune_tumor_antigen_fractions 0)]
   [:span.mx-2.pt-2 "plus"]
   ])

;;; In multitool
(defn keyword-conc
  [& parts]
  (keyword (str/join "-" (map name parts))))

;;; TODO propagate up into earlier guys
;;; TODO subfeature name not actually used, but could be a tooltip or something
(defn subfeature-selector
  [feature-type subfeature-name template-position]
  (let [subfeatures (subfeature-values feature-type template-position)
        param-key (keyword-conc feature-type (str template-position))] ;  subfeature-name
    (when-not (empty? subfeatures)
      ;; Encodes the position in the parameter keyword for later extraction. Hacky but simple.
      (select-widget-minimal param-key subfeatures))))

(defn boilerplate
  [s]
  [:span.mx-2.pt-2 s])

(defn echo
  [feature-type subfeature-name]
  (let [value @(rf/subscribe [:param :features (keyword-conc feature-type subfeature-name)])]
    [:span.mx-2.pt-2 value]))

(defn feature-template
  [feature-type]
  (-> data/feature-ui-master
      (get (name feature-type))
      first                             ;this gets the first size entry, ideally those will go away
      second
      ))

;;; Perhaps this should be a subscription? Yes
(rf/reg-sub
 :selected-feature
 (fn [db _]
  ;; stuff into query machinery
  ;; (should be) A method since I'm guessing there may be exceptions to the general rule
  (let [feature-type (keyword (get-in db [:params :features :feature-bio-feature-type]))
        feature-params (get-in db [:params :features])
        template (feature-template feature-type)
        ;; join into feature name
        elements
        (map (fn [item i]
               (if (= 1 (count item))
                 (first item)           ;boilerplate
                 (get feature-params  (keyword-conc feature-type (str i)))   ;a feature, read out of params
                 ))
             template (range))
        feature (str/join "_" elements)]
    feature)))

(defmethod feature-ui :immune_cell_functional_relative_to_all_tumor
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_functional_relative_to_all_tumor :functional_marker 1)
   (subfeature-selector :immune_cell_functional_relative_to_all_tumor :cell-type 0)
   (boilerplate "over all tumor count")
   ])

(defmethod feature-ui :immune_cell_func_tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :functional_marker 1)
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :cell-type 0)
   (boilerplate "over")
   (echo :immune_cell_func_tumor_antigen_fractions :functional_marker)
   (boilerplate "plus")
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :relative 5)
   ])

(defn l2-nonspatial
  []
  [:div 
   (select-widget :feature-type (map first non-spatial-features-2-3))
   (let [feature-l2 @(rf/subscribe [:param :features :feature-type])]
     (if (= "marker_intensity" feature-l2)
       [:div
        (select-widget :feature-meta-cluster cell-meta-clusters)
        [l3-feature]]
       ;; Not marker_intensity
       [:div
        (select-widget :feature-bio-feature-type (get (into {} non-spatial-features-2-3) feature-l2))
        (when-let [bio_feature_type @(rf/subscribe [:param :features :feature-bio-feature-type])]
          [:div
           [feature-ui bio_feature_type]
           (when-let [l4-features @(rf/subscribe [:data [:features {:bio_feature_type bio_feature_type}]])]

             [:div [:h6 "OBSOLETE"]
              (select-widget :feature-feature l4-features #(rf/dispatch [:set-param :universal :feature %]))
              ;; Off until I get some feedback from Stanford
              #_
              [:div.row
               [:div.col-4.px-4 "segmented"]
               [:div.col-10
                (segmented-selector l4-features)]]])
           ]

          )]))])

(defn ui
  []
  [:div
   [:div.row
    [:div.col-10
     [select-widget :feature-supertype [:non-spatial :spatial]]
     (if (= "non-spatial" @(rf/subscribe [:param :features :feature-supertype]))
       [l2-nonspatial]
       [l2-spatial])
     [:h6] "Feature: " @(rf/subscribe [:selected-feature])]]
   #_                                   ;They didn't like this so much
   [:div.row
    [:h4 "Alt menu"]
    (wu/select-widget
     :feature-type-exp
     nil
     #(rf/dispatch [:foo])
     (cons "marker intensity"
           (map (fn [[c l]] {:optgroup (wu/humanize c) :options (map wu/humanize l)})
                (rest non-spatial-features-2-3)))
     "hierarchical type select")]])

