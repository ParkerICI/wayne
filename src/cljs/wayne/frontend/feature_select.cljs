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

(rf/reg-sub
 :selected-feature
 (fn [db _]
   ;; stuff into query machinery
   (let [feature (get-in db [:params :features :feature-feature_variable])]
     (when-not (= feature (get-in db [:params :universal :feature]))
       (rf/dispatch [:set-param :universal :feature feature]))
     feature)))

;;; Note: might be easier and less error-prone to build this from data
(def nonspatial-feature-tree
  [
   ["Cells"
    ["Cell_Abundance"
     ["Relative_to_all_tumor_cells"]
     ["Relative_to_all_immune_cells"]]
    ["Cell_Ratios"
     ["Cells_and_functional_markers"]
     ["Immune_cells"]
     ["Immune_to_Tumor_cells"]
     ["Tumor_cells"]]
    ]
   ["Protein"
    ["Tumor_Antigens_Intensity_Segments"]
    ["Tumor_Antigens_Intensity"]        ;TODO metacluster
    ["Functional_marker_intensity"]     ;ditto
    ["Phenotype_marker_intensity"]      ;dito
    ]
   ["Glycan"
    ["Relative_Intensity"]]
   ])

(def spatial-feature-tree
  [["RNA"
    ["Immune_High"]
    ["Immune_Low"]]
   ["Cells"
    ["Neighborhood_Frequencies"]          ;?db
    ["Spatial_Density"]
    ["Area_Density"
     ]]           ;db  "Cells_and_functional_markers" ?
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
        l2-feature @(rf/subscribe [:param :features :feature-broad_feature_type])
        l3-feature-tree (rest (u/some-thing #(= (first %) l2-feature) l2-feature-tree))
        l3-feature @(rf/subscribe [:param :features :feature-feature_type])
        l4-feature-tree (rest (u/some-thing #(= (first %) l3-feature) l3-feature-tree))
        l4-feature (and (not (empty? l4-feature-tree))
                        @(rf/subscribe [:param :features :feature-bio-feature-type]))
        query-feature (or l4-feature l3-feature)
        selected-feature @(rf/subscribe [:selected-feature])
        ]
    (prn :feature-selector query-feature l1-feature l2-feature l3-feature l4-feature)
    [:div
     (select-widget :feature-supertype (map first feature-tree))
     (select-widget :feature-broad_feature_type (map first l2-feature-tree))
     (select-widget :feature-feature_type (map first l3-feature-tree))
     (when-not (empty? l4-feature-tree)
       (prn :l4-feature-tree l4-feature-tree)
       (select-widget :feature-bio-feature-type (map first l4-feature-tree)))
     (if (contains? #{"immune-high" "immune-low"} query-feature)
       (row "RNA" [autocomplete/ui])
       (select-widget :feature-feature_variable @(rf/subscribe [:data :features {:bio_feature_type query-feature}])))
     (row "selected" selected-feature)
     ]) )


;;; → Multitool?
(defn conjs
  [coll thing]
  (if (nil? coll)
    #{thing}
    (conj coll thing)))


(defn feature-list-ui
  []
  (let [feature @(rf/subscribe [:selected-feature])
        feature-list @(rf/subscribe [:param :heatmap2 :feature-list])]
    [:div
     [row "actual variable"
      [:span
       (wu/humanize feature)
       (when (not (contains? feature-list feature))
         [:button.btn.btn-sm.btn-secondary.mx-2 ;TODO none of these boostrap stules are present
          {:href "#"
           :on-click #(rf/dispatch [:update-param :heatmap2 :feature-list conjs feature])}
          "add"])
       ]]
     ;; TODO lozenge UI
     [row
      [:span "feature list "
       (when-not (empty? feature-list)
         [:button.btn.btn-sm.btn-secondary.mx-2 {:href "#" :on-click #(rf/dispatch [:set-param :heatmap2 :feature-list #{}])} "clear"])]
      (str/join ", " (map wu/humanize feature-list))]]))

(defn ui
  []
  [:div.row
   [:div.col-10
    [new-feature-selector]
    ]])

