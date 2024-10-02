(ns wayne.frontend.munson
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [com.hyperphor.way.web-utils :as wu]
   [com.hyperphor.way.ui.init :as init]
   [wayne.frontend.universal :as universal]
   [wayne.frontend.feature-select :as fui]
   [org.candelbio.multitool.core :as u]
   ))


;;; This is universal.cljs, but adapted to run in Munson website.


(def dims
  {:Tumor_Diagnosis "Final Diagnosis"
   :WHO_grade "WHO grade"
   :Tumor_Region "Tumor Region"
   :recurrence "Recurrence"
   :IDH_R132H_Status "IDH Status"
   :treatment "Treatment"
   ;; New
   :Immunotherapy "Immunotherapy"
   :Longitudinal "Longitudinal"
   :Sex "Sex"
   :Progression "Progression"
   })

;;; Very non-re-frame sue me
(defn toggle
  [id class]
  (let [elt (.getElementById js/document id)]
    (.toggle (.-classList elt) class)))

(defn open
  [id class]
  (let [elt (.getElementById js/document id)]
    (.remove (.-classList elt) class)))

(defn dim-selector
  [dim icon-url active-dim]
  (let [text (get dims dim)]
    [:div.dataset-tag 
     {;; :data-icon data-icon
      :class (when (= dim active-dim) "dataset-tag-active")
      :on-click #(do
                   (rf/dispatch [:set-param :universal :dim dim])
                   (rf/dispatch [:set-param :heatmap2 :dim dim])
                   (open "collapser-feature" "select-form-group")
                   (open "collapser-viz" "select-form-group") ;TODO maybe open only on feature selection
                   )}
     (when icon-url
       [:img.icon {:src icon-url, :alt text}])
     text]))


(def filter-features
  '{:IDH_R132H_Status ("Mutant" "Unknown" "Wild_type"),
    :Longitudinal ("NA" "No" "Yes"),
    :treatment
    ("Combinatorial_CD27_and_SPORE_Vaccine"
     "Lysate_Vaccine"
     "Neoadjuvant_PD1_Trial_1"
     "Neoadjuvant_PD1_Trial_2"
     "SPORE_Vaccine"
     "Treatment_Naive"),
    :Tumor_Region ("Other" "Tumor_core" "Tumor_core_to_infiltrating" "Tumor_infiltrating"),
    :Sex ("F" "M" "Unknown"),
    :Progression ("No" "No_later_event" "Unknown" "Yes" "Yes_later_event"),
    :recurrence ("No" "Unknown" "Yes"),
    :Immunotherapy ("false" "true"),
    :Tumor_Diagnosis
    ("Astrocytoma"
     "GBM"
     "GBM_other"
     "NA"
     "Oligodendroglioma"
     "PXA"
     "Pediatric DIPG"
     "Pediatric HGG (other)"),
    :WHO_grade ("2" "3" "4" "NA" "Unknown")})



(defn filter-values-ui
  [id dim]
  (let [all-values (get filter-features dim)
        feature @(rf/subscribe [:param :universal [:feature]])
        filters @(rf/subscribe [:param :universal [:filters]])
        ;; TODO this ends up accumulating a lot in the db, I don't think its fatal but
        ;; TODO also filter needs to be cleaned/regularized for matching
        in-values @(rf/subscribe [:data [:populate {:dim dim :feature feature :filters filters}]])
        ] 
    [:div.accordian-panel {:id id}
     (for [value all-values
           :let [id (str "dim" (name dim) "-" value)
                 checked? (get-in filters [dim value])
                 disabled? (not (contains? in-values value))
                 ]]
       [:label.custom-checkbox
        [:input {:type "checkbox",
                 :hidden "hidden",
                 :_ "_"
                 :key id
                 :id id
                 :disabled (if disabled? "disabled" "")
                 :checked (if checked? "checked" "")
                 :on-change (fn [e]
                              (rf/dispatch
                               [:set-param :universal [:filters dim value] (-> e .-target .-checked)])
                              (rf/dispatch
                               [:set-param :heatmap2 [:filter dim value] (-> e .-target .-checked)])) ;NOTE: used to be :heatmap and needed for the older versions
                 }]
        [:span.checkmark]
        [:label.form-check-label {:for id :class (when disabled? "text-muted")
                                  }
         (wu/humanize value) disabled?]])
     ]))



(defn filter-ui
  []
  [:div.filters
   [:h3.mb-30.font-bold.filter-subheader "Filter"]
   [:div.filter-list
    (for [dim (keys filter-features)]
      (let [collapse-id (str "collapse" (name dim))
            heading-id (str "heading" (name dim))]
        [:div.accordian.accordian-collapsed ;TODO collapsed maybe not
         [:div.accordian-title
          [:h3 (wu/humanize dim)]
          [:img {:src "../assets/icons/minus-grey.svg"
                 :on-click #(toggle collapse-id "select-form-group") ;Note: using this css style for display: none;
                 }]] ;TODO
         [filter-values-ui collapse-id dim]
         ]))]])

(defn filter-view
  []
  [:div.selected-filter-list
   (u/mapf (fn [[col vals]]
             (let [in-vals (u/mapf (fn [[v in]]
                                     (if in v))
                                   vals)]
               ;; TODO :span not quite right, and maybe we want to show col/dim?
               ;; TODO implement close icon
               [:span (map (fn [v] [:div.tag (wu/humanize v) [:img {:src "../assets/icons/close.svg"}]]) in-vals)]))
           @(rf/subscribe [:param :universal [:filters]]))
   [:button.clear-all-button {:type "submit"
                              :on-click #(do (rf/dispatch [:set-param :universal :filters {}])
                                             (rf/dispatch [:set-param :heatmap :filter {}])
                                             (rf/dispatch [:set-param :universal :feature nil]))
                              } "Clear All"]])


(defn collapse-panel
  [id title content]
  (let [id (str "collapser-" (name id))]
    [:div.featured-view.relative
     [:div.features-view
      [:div.features-view-header
       [:div.flex.align-center.flex-row.gap-8 [:h3 title]]
       [:div.flex.gap-16
        [:img#toggleSelectForm {:src "../assets/icons/merge-horizontal-grey.svg"
                                :on-click #(toggle id "select-form-group")
                                }]
        ;; TODO prob don't want this
        [:img {:src "../assets/icons/download.svg"}]]]
      [:div.select-form-group.mt-24 {:id id}              ;.select-form-group if start collapsed
       content
       ]]]))

(defn munson-new
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        filters @(rf/subscribe [:param :universal [:filters]])
        data @(rf/subscribe [:data [:universal {:feature feature :filters filters :dim dim}]])]
    [:section.query-builder-section
     [:div.container
      [:div.query-builder-content
       [:div.filters-view
        [:div.query-builder-content-headline [:h1 "Query Builder"]]
        [:div.dataset-selection
         [:h3.mb-30.font-bold "Compare across"]
         [:div.dataset-tags
          [dim-selector :Tumor_Diagnosis "../assets/icons/diagnosis-icon.svg" dim]
          [dim-selector :WHO_grade "../assets/icons/question-icon.svg" dim]
          [dim-selector :Tumor_Region "../assets/icons/roi-icon.svg" dim]
          [dim-selector :recurrence "../assets/icons/recurrence-icon.svg" dim]
          [dim-selector :IDH_R132H_Status "../assets/icons/file-chart-icon.svg" dim]
          [dim-selector :treatment "../assets/icons/treatment-icon.svg" dim]
          [dim-selector :Sex nil dim]
          [dim-selector :Immunotherapy nil dim]
          [dim-selector :Longitudinal nil dim]
          [dim-selector :Progression nil dim]
          ]]
        [:div.divider.mt-30.mb-30]
        [filter-ui]]
       [:div.pt-20
        [:p.query-builder-section-subheadline
         "Run sensitivity analyses to explore insights in cancer research, from clinical trials to innovative therapies."]
        [:div.selected-filter-wrapper
         [collapse-panel
          :dim
          (or (get dims @(rf/subscribe [:param :universal :dim]))
              "←  Select a dimension")
          [:div#selectedFilterView
           [filter-view]
           [:div.divider.mb-24.mt-24]
           ;; Static heatmap
           #_ [:img {:src "../assets/images/graph-frame.png"}]
           ]
         ]

         [collapse-panel :feature "Feature Selection"
          (if dim
            [:div {:style {:width "500px"}} ;TODO
             (when-not feature
               [universal/feature-second-warning])
             [fui/ui]]
            [universal/dim-first-warning])]

         [collapse-panel :viz [:span "Visualization " (when @(rf/subscribe [:loading?])
       (wu/spinner 1))]
          [:div#visualization.visualization-container
           [:div
            [universal/visualization dim feature data]
            #_
            [:div.no-data.text-center
             [:img {:src "../assets/icons/empty-box.svg"}]
             [:h3 "No Data Found"]
             [:p "Enter or adjust your filters to see data."]]]]]]]]]])
  )

(defn app-ui
  []
  [:div
   ;; Stand in
   [munson-new]
   #_ [universal/ui]
   ])

(defn ^:export init
  []
  (init/init app-ui nil)
  )
