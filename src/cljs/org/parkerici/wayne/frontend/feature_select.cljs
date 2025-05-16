(ns org.parkerici.wayne.frontend.feature-select
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [com.hyperphor.way.web-utils :as wu]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            [org.parkerici.wayne.frontend.autocomplete :as autocomplete] ;TEMP
            [org.parkerici.wayne.frontend.utils :as wwu]
            [org.parkerici.wayne.data-defs :as dd]
            )
  )

;;; ⊛✪⊛ Data defintions ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

;;; Moved to data_defs.cljc


;;; ⊛✪⊛ Utilities ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

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

;;; NOTE used to use :set-param-if which didn't work well, I think that can be removed from Way
(defn select-widget-minimal
  [id values & [extra-action believe-param?]]
  (let [current-value @(rf/subscribe [:param :features id])]
    (when (and (not (empty? values))
               (not (contains? (set values) current-value)))
      (rf/dispatch [:set-param :features id (safe-name (if believe-param? ;Another epicycle, ensures this works for examples where everything gets set at once. 
                                                         (or current-value (first values))
                                                         (first values)))
                                                         ]))
    [:span {:style {:width "80%"}}
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
      {:display "inherit" :width "inherit" :margin-left "2px"})
     (when-let [d (get dd/feature-definitions current-value)]
       (wwu/info d))])) ;TODO tooltips

(defn select-widget
  [id values & [extra-action believe-param?]]
  [row
   (wu/humanize (trim-prefix id))
   (select-widget-minimal id values extra-action believe-param?)
   ])

;;; Hack and temporary

(defn clean-select-value
  [v]
  (if (= v "---")
    nil
    v))

;;; ⊛✪⊛ Segmented features ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

(defn boilerplate
  [s]
  [:span.mx-2.text-nowrap s])

(defn subfeature-selector
  [subfeatures i]
  (let [;; subfeatures (subfeature-values feature-type template-position)
        ;; subfeatures (if nullable? (cons nil subfeatures) subfeatures)
        ;; Encodes the position in the parameter keyword for later extraction. Hacky but simple.
        param-key (u/keyword-conc :subfeature (str i))]
    (select-widget-minimal param-key subfeatures)))

(defn segmented
  [template]
  [:div.border.p-2 {:style { :display "inline-flex"}}
   (map (fn [elt i]
          (cond (string? elt) (boilerplate elt)
                (sequential? elt) [subfeature-selector elt i] ;sadly no subfeature name but it isn't used for anything anyway
                :else (str elt)))
        template
        (range))])

;;; Methods for customizing the bottom level of feature select tree
;;; Note: these aren't parallel becau

(defmulti feature-variable-ui (fn [feature-type bio-feature-type] [feature-type bio-feature-type]))

(defmethod feature-variable-ui :default
  [feature-type bio-feature-type]
  (select-widget :feature-feature_variable
                 @(rf/subscribe [:data :features {:feature_type feature-type
                                                  :bio_feature_type bio-feature-type}])
                 nil
                 true                   ;kludge so this works with examples
                 ))

(defmulti feature-from-db
  (fn [db]
    [(get-in db [:params :features :feature-feature_type])
     (get-in db [:params :features :feature-bio-feature-type])]))

(defmethod feature-from-db :default
  [db]
  (get-in db [:params :features :feature-feature_variable]))

(defmethod feature-variable-ui ["Immune_High" nil]
  [_ _]
  (row "RNA" [autocomplete/ui]))

(defmethod feature-from-db :default
  [db]
  (get-in db [:params :features :feature-feature_variable]))

(defmethod feature-variable-ui ["Immune_Low" nil]
  [_ _]
  (row "RNA" [autocomplete/ui]))

(defmethod feature-variable-ui ["Cell_Ratios" "Cells_and_functional_markers"]
  [_ _]
  (row "feature_variable" 
       [segmented [(first dd/cells-and-functional-marker-segs)
                   "over"
                   (wu/humanize @(rf/subscribe [:param :features :subfeature-0]))
                   "plus"
                   (second dd/cells-and-functional-marker-segs)
                   ]]))

(defn joins
  [& segs]
  (str/join "_" segs))

(defmethod feature-from-db ["Cell_Ratios"  "Cells_and_functional_markers"]
  [db]
  (joins (get-in db [:params :features :subfeature-0])
         "over"
         (get-in db [:params :features :subfeature-0])
         "plus"
         (get-in db [:params :features :subfeature-4])
         "func"))

(defmethod feature-variable-ui ["Neighborhood_Frequencies" nil]
  [_]
  (row "feature_variable"
       [segmented
        [dd/neighborhoods
         "-"
         dd/neighborhoods]]))

(defmethod feature-from-db ["Neighborhood_Frequencies" nil]
  [db]
  (str (get-in db [:params :features :subfeature-0])
       "-"
       (get-in db [:params :features :subfeature-2])))
    
;;; ⊛✪⊛ UI and data ✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪⊛✪

(rf/reg-sub
 :selected-feature
 (fn [db _]
   ;; stuff into query
   (let [feature (feature-from-db db)]
     (when-not (= feature (get-in db [:params :universal :feature]))
       (rf/dispatch [:set-param :universal :feature feature]))
     feature)))

(defn new-feature-selector
  []
  (let [l1-feature @(rf/subscribe [:param :features :feature-supertype])
        l2-feature-tree (rest (u/some-thing #(= (first %) l1-feature) dd/feature-tree))
        l2-feature @(rf/subscribe [:param :features :feature-broad_feature_type])
        l3-feature-tree (rest (u/some-thing #(= (first %) l2-feature) l2-feature-tree))
        l3-feature @(rf/subscribe [:param :features :feature-feature_type])
        l4-feature-tree (rest (u/some-thing #(= (first %) l3-feature) l3-feature-tree))
        l4-feature (if (empty? l4-feature-tree)
                     (do (rf/dispatch [:set-param :features :feature-bio-feature-type nil]) nil)
                     @(rf/subscribe [:param :features :feature-bio-feature-type]))
        ]
    [:div
     (select-widget :feature-supertype (map first dd/feature-tree))
     (select-widget :feature-broad_feature_type (map first l2-feature-tree))
     (select-widget :feature-feature_type (map first l3-feature-tree))
     (when-not (empty? l4-feature-tree)
       (select-widget :feature-bio-feature-type (map first l4-feature-tree)))
     (feature-variable-ui l3-feature l4-feature)
     (row "selected" @(rf/subscribe [:selected-feature]))
     ]))

(defn ui
  []
  [:div.row
   [:div.col-10
    [new-feature-selector]
    ]])

