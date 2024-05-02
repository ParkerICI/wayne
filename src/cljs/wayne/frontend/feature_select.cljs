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

(defn row
  [label contents]
  [:div.row
   [:div.col-3 [:label.small.pt-2 [:b label]]]  ;TEMP for dev I think
   [:div.col-7 contents]])

(defn select-widget
  [id values & [extra-action]]
  #_
  (when (and (not (empty? values)) (first values))
    (rf/dispatch [:set-param-if :features id (name (first values))])) ;TODO smell? But need to initialize somewhere
  [row
   (wu/humanize (trim-prefix id))
   (wu/select-widget
     id
     @(rf/subscribe [:param :features id])
     #(do
        (rf/dispatch [:set-param :features id %])
        (when extra-action (extra-action %) )) ;TODO ugh
     (cons {:value nil :label "---"}
         (map (fn [v] {:value v :label (wu/humanize v)}) values))
     nil)])

;;; TODO DRYify with above
(defn select-widget-minimal
  [id values]
  ;; Something wrong, smelly about this
  #_
  (when (and (not (empty? values)) (first values))
    ;; -if removal seems to fix things? This is wrong and breaks updates
    (rf/dispatch [:set-param-if :features id (name (first values))])) ;TODO smell? But need to initialize somewhere
  (wu/select-widget
   id
   @(rf/subscribe [:param :features id])
   #(rf/dispatch [:set-param :features id %]) ;Necessary to allow changes! But also does some kind of gross invalidation
   (cons {:value nil :label "---"}
         (map (fn [v] {:value v :label (wu/humanize v)}) values))
   nil
   nil
   {:display "inherit" :width "inherit" :margin-left "2px"})) ;TODO tooltips

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

(defn subfeature-values
  [feature-type position]
  (-> data/feature-ui-master
      (get (name feature-type))
      first                             ;this gets the first size entry, ideally those will go away
      second
      (nth position)))

;;; In multitool
(defn keyword-conc
  [& parts]
  (keyword (str/join "-" (map name parts))))

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
                                 (keyword-conc feature-type (str k))
                                 (get params (keyword-conc feature-type (str v)))
                                 ))
            params
            repeats)))

(rf/reg-sub
 :selected-feature
 (fn [db _]
  ;; stuff into query machinery
  ;; (should be) A method since I'm guessing there may be exceptions to the general rule
  (let [feature-type (keyword (get-in db [:params :features :feature-bio-feature-type]))
        feature-params (get-in db [:params :features])
        feature-params (extend-params feature-type feature-params)
        template (feature-template feature-type)
        ;; join into feature name
        elements
        (map (fn [item i]
               (if (= 1 (count item))
                 (first item)                                             ;boilerplate
                 (get feature-params (keyword-conc feature-type (str i))) ;a feature, read out of params
                 ))
             template (range))
        feature (str/join "_" elements)]
    ;; NOTE Connects up to query machinery. Not completely sure if this is kosher, but it seems to work
    (when-not (= feature (get-in db [:params :universal :feature]))
      (rf/dispatch [:set-param :universal :feature feature]))
    feature)))

(defn feature-valid?
  [feature]
  (contains? data/feature-names feature))

;;; TODO propagate up into earlier guys
;;; TODO subfeature name not actually used, but could be a tooltip or something
(defn subfeature-selector
  [feature-type subfeature-name template-position]
  (let [subfeatures (subfeature-values feature-type template-position)
        param-key (keyword-conc feature-type (str template-position))] ;  subfeature-name
    (when-not (empty? subfeatures)
      ;; Encodes the position in the parameter keyword for later extraction. Hacky but simple.
      (select-widget-minimal param-key subfeatures))))

(defn subfeature-selector-literal
  [feature-type subfeature-name template-position subfeatures]
  (let [;subfeatures (subfeature-values feature-type template-position)
        param-key (keyword-conc feature-type (str template-position))] ;  subfeature-name
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
  (let [subfeature-param (keyword-conc feature-type (str boss-pos))]
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

;;; TODO feature name generation needs to be more complex in this case.
(defmethod feature-ui :tumor_antigen_spatial_density
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :tumor_antigen_spatial_density :antigen1 0]
   (let [antigen1 @(rf/subscribe [:param :features :tumor_antigen_spatial_density-0])]
     (when antigen1                     
       ;; TODO could exclude antigen1
       [subfeature-selector :tumor_antigen_spatial_density :antigen2 2]
       ))])

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
   (subfeature-selector :immune_cell_functional_relative_to_all_tumor :functional_marker 1)
   (subfeature-selector :immune_cell_functional_relative_to_all_tumor :cell-type 0)
   (boilerplate "over all tumor count")
   (subfeature-selector :immune_cell_functional_relative_to_all_immune :cell-type 0)   ])

(defmethod feature-ui :immune_cell_func_tumor_antigen_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :functional_marker 1)
   (subfeature-selector :immune_cell_func_tumor_antigen_fractions :cell-type 0)
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

;;; TODO This one has 4 parts, but we have 2
#_
(defmethod feature-ui :immune_functional_marker_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :immune_functional_marker_fractions :cell-type-1 0]
   [subfeature-selector :immune_functional_marker_fractions :functional-marker-1 2]
   [boilerplate "over"]
   [boilerplate @(rf/subscribe [:param :features :immune_functional_marker_fractions-0])]
   [boilerplate @(rf/subscribe [:param :features :immune_functional_marker_fractions-2])]
   [boilerplate "plus"]
   [subfeature-selector :immune_functional_marker_fractions :cell-type-2 4]
   ;; TODO needs breaking up
   #_ [subfeature-selector :immune_functional_marker_fractions :functional-marker-1 5]
   ])

;;; 2 part version, works but not what was speced
(defmethod feature-ui :immune_functional_marker_fractions
  [_]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   [subfeature-selector :immune_functional_marker_fractions :combo-1 0]
   [boilerplate "over"]
   (repeater :immune_functional_marker_fractions 0 2)
   [boilerplate "plus"]
   [subfeature-selector :immune_functional_marker_fractions :combo-2 4]
   ])

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
        (select-widget :feature-bio-feature-type (get (into {} non-spatial-features-2-3) feature-l2))
        (when-let [bio_feature_type @(rf/subscribe [:param :features :feature-bio-feature-type])]
          [:div
           [row "feature" [feature-ui bio_feature_type]] 
           (let [feature @(rf/subscribe [:selected-feature])]
             [row "actual"
                  [:span
                   feature 
                   [:b (str " " (if (feature-valid? feature) "valid" "nope") )]]])
           ;; Hah don't need this any more
           #_
           (when-let [l4-features @(rf/subscribe [:data [:features {:bio_feature_type bio_feature_type}]])]
             [:div [:h5 "OBSOLETE"]
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
     ]]
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

