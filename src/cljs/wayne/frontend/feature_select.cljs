(ns wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [com.hyperphor.way.web-utils :as wu]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.autocomplete :as autocomplete] ;TEMP
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

(defn clean-select-value
  [v]
  (if (= v "---")
    nil
    v))

;;; This does the work of computing the feature based on the various selectors
;;; via compute-feature-variable (multimethod)
#_
(rf/reg-sub
 :selected-feature
 (fn [db _]
   ;; stuff into query machinery
   (when (get-in db [:params :features :feature-type])
     (let [feature
           (if (= "marker_intensity" (get-in db [:params :features :feature-type]))
             (get-in db [:params :features :feature-feature])
             (let [feature-type (keyword (get-in db [:params :features :feature-bio-feature-type]))
                   feature-params (->> (get-in db [:params :features])
                                       (u/map-values clean-select-value)
                                       (extend-params feature-type))]
               (when (and feature-type feature-params)
                 (compute-feature-variable feature-type feature-params))))] ;methodized
       ;; NOTE Connects up to query machinery. Not completely sure if this is kosher, but it seems to work
       (when-not (= feature (get-in db [:params :universal :feature]))
         (rf/dispatch [:set-param :universal :feature feature]))
       feature))))

(defn feature-valid?
  [feature]
  #_ (contains? fn/feature-names feature)
  true)

;;; TODO propagate up into earlier guys
;;; TODO subfeature name not actually used, but could be a tooltip or something

(def nonspatial-feature-tree
  [["Glycan"
    ["relative_intensity"]]
   ["Cells"
    ["Cell_Ratios"
     ["Cells_and_functional_markers"]
     ["Immune_cells"]
     ["Immune_to_Tumor_cells"]
     ["Tumor_cells"]]
    ["cell_abundances"
     ["Relative_to_all_tumor_cells"]
     ["Relative_to_all_immune_cells"]]]
   ["Protein"
    ["Tumor_Antigens_Intensity_Segments"]
    ["Tumor_Antigens_Intensity"]        ;TODO metacluster
    ["Functional_marker_intensity"]     ;ditto
    ["Phenotype_marker_intensity"]      ;dito
    ]])

(def spatial-feature-tree
  [["RNA"
    ["immune-high"]
    ["immune-low"]]
   ["Glycans"
    ["Pixel clusters"]]                 ;?db I thik it's "glycan" and this is a no-op
   ["Cells"
    ["Neighborhood_Frequencies"]          ;?db
    ["spatial_density"]
    ["Area Density"
     ["Tumor_cells"]
     ["Immune_cells"]
     ["Functional_markers"]]]           ;db  "Cells_and_functional_markers" ?
   ]
  )

(def feature-tree
  `[["nonspatial" ~@nonspatial-feature-tree]
    ["spatial" ~@spatial-feature-tree]
    ])


(defn new-feature-selector
  []
  (let [l1-feature @(rf/subscribe [:param :features :feature-supertype])
        l2-feature-tree (rest (u/some-thing #(= (first %) l1-feature) feature-tree))
        l2-feature @(rf/subscribe [:param :features :feature-broad-feature-type])
        l3-feature-tree (rest (u/some-thing #(= (first %) l2-feature) l2-feature-tree))
        l3-feature @(rf/subscribe [:param :features :feature-feature-type])
        l4-feature-tree (rest (u/some-thing #(= (first %) l3-feature) l3-feature-tree))
        l4-feature (and (not (empty? l4-feature-tree))
                        @(rf/subscribe [:param :features :feature-bio-feature-type]))
        query-feature (or l4-feature l3-feature)
        ]
    (prn :feature-selector query-feature l1-feature l2-feature l3-feature l4-feature)
    [:div
     (select-widget :feature-supertype (map first feature-tree))
     (select-widget :feature-broad-feature-type (map first l2-feature-tree))
     (select-widget :feature-feature-type (map first l3-feature-tree))
     (when-not (empty? l4-feature-tree)
       (prn :l4-feature-tree l4-feature-tree)
       (select-widget :feature-bio-feature-type (map first l4-feature-tree)))
     (if (contains? #{"immune-high" "immune-low"} query-feature)
       (row "RNA" [autocomplete/ui])
        (select-widget :feature_feature_variable @(rf/subscribe [:data :features {:bio_feature_type query-feature}])))
     ]) )


;;; → Multitool?
(defn conjs
  [coll thing]
  (if (nil? coll)
    #{thing}
    (conj coll thing)))

#_
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
           :on-click #(rf/dispatch [:update-param :heatmap2 :feature-list conjs feature])}
          "add"])
       ]]
     ;; TODO lozenge UI
     [row
      [:span "feature list "
       (when-not (empty? feature-list)
         [:a {:href "#" :on-click #(rf/dispatch [:set-param :heatmap2 :feature-list #{}])} "clear"])]
      (str/join ", " (map wu/humanize feature-list))]]))

(defn ui
  []
  [:div
   [:div.row
    [:div.col-10
     [new-feature-selector]
     ;; Note: if this is omitted, initial plot doesn't load. So if people don't liek it, keep it but hide it
     (when-let [feature @(rf/subscribe [:selected-feature])]
       [row "feature_variable"
        [:span
         (wu/humanize feature)
         [:b (str " " (if (feature-valid? feature) "present " "ND") )] ;TODO EnJun wants to wordsmith these

         ]])     
     ]]
   ])

