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
   [wayne.frontend.autocomplete :as autocomplete] ;TEMP
   ))

;;; This is universal.cljs, but adapted to run in Munson website.

;;; Quick and dirty, could be improved
(defn info
  [text]
  [:img {:src "https://www.iconpacks.net/icons/3/free-information-icon-6276.png"
         :height 20
         :title text
         :data-toggle "tooltip"
         :style {:cursor "pointer"
                 :margin-left "5px"}
         }])

(def dims
  (array-map                            ; Order is important 
   :Tumor_Diagnosis {:label "Final Diagnosis"
                     :icon "diagnosis-icon.svg"
                     :values ["Astrocytoma"
                              "GBM"
                              "GBM_other"
                              "Oligodendroglioma"
                              "PXA"
                              "Pediatric DIPG"
                              "Pediatric HGG (other)"]}
   :WHO_grade {:label "WHO grade"
               :icon "question-icon.svg"
               :values  ["2" "3" "4" "Unknown"]}
   :Immunotherapy {:label "Immunotherapy"
                   :info "Treatment status"
                   :values [["false" "No"] ["true" "Yes"]]}
   :treatment {:label "Treatment"
               :info "Various pre-sample collection treatments"
               :icon "treatment-icon.svg"
               :values ["Combinatorial_CD27_and_SPORE_Vaccine"
                        "Lysate_Vaccine"
                        "Neoadjuvant_PD1_Trial_1"
                        "Neoadjuvant_PD1_Trial_2"
                        "SPORE_Vaccine"
                        "Treatment_Naive"]}
   :recurrence {:label "Recurrence"
                :info "Recurrent tumor"
                :icon "recurrence-icon.svg"
                :values ["No" "Yes"]}
   :Longitudinal {:label "Longitudinal"
                  :info "Patient samples with paired primary and recurrent events"
                  :values ["Yes" "No"]}
   :Progression {:label "Progression"
                 :info "Patients that progressed from lower grade to higher grades. later event catagories denotes the recurrent tumor."
                 :values ["No" "No_later_event" "Yes" "Yes_later_event"]}
   :Tumor_Region {:label "Tumor Region"
                  :info "Anatomical regions of the tumor"
                  :icon "roi-icon.svg"
                  :values   ["Other" "Tumor_core" "Tumor_core_to_infiltrating" "Tumor_infiltrating"]}
   :IDH_R132H_Status {:label "IDH Status"
                      :info "Common IDH mutation"
                      :icon "file-chart-icon.svg"
                      :values ["Mutant" "Wild_type"]
                      }
   :Sex {:label "Sex"
         :values [["F" "Female"] ["M" "Male"] "Unknown"]}
   ))

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
  [dim active-dim]
  (let [icon (get-in dims [dim :icon])
        text (get-in dims [dim :label])]
    [:div.dataset-tag 
     {;; :data-icon data-icon
      :class (when (= dim active-dim) "dataset-tag-active")
      :on-click #(do
                   (rf/dispatch [:set-param :universal :dim dim])
                   (rf/dispatch [:set-param :heatmap2 :dim dim])
                   (open "collapser-feature" "collapsed")
                   (open "collapser-viz" "collapsed") ;TODO maybe open only on feature selection
                   )}
     (when icon
       [:img.icon {:src (str "../assets/icons/" icon), :alt text}])
     text]))

(defn filter-values-ui
  [id dim]
  (let [all-values (get-in dims [dim :values])
        feature @(rf/subscribe [:param :universal [:feature]])
        filters @(rf/subscribe [:param :universal [:filters]])
        ;; TODO this ends up accumulating a lot in the db, I don't think its fatal but
        ;; TODO also filter needs to be cleaned/regularized for matching
        in-values @(rf/subscribe [:data [:populate dim] {:dim dim :feature feature :filters filters}])
        ] 
    [:div.accordian-panel.collapsed {:id id}
     (for [value-spec all-values
           :let [value (if (vector? value-spec) (first value-spec) value-spec)
                 label (wu/humanize (if (vector? value-spec) (second value-spec) value-spec))
                 id (str "dim" (name dim) "-" value)
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
         label disabled?]])
     ]))

(defn filter-ui
  []
  [:div.filters
   [:h3.mb-30.font-bold.filter-subheader
    "Filter" [info "Select molecular and clinical criteria to filter the data for visualization"]]
   [:div.filter-list
    (for [dim (keys dims)]
      (let [collapse-id (str "collapse" (name dim))]
        [:div.accordian.accordian-collapsed
         [:div.accordian-title {:on-click #(toggle collapse-id "collapsed")}
          [:h3 (get-in dims [dim :label])
           (when-let [info-text (get-in dims [dim :info])]
             [info info-text])]
          [:img {:src "../assets/icons/minus-grey.svg"}]]
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
                                :on-click #(toggle id "collapsed")
                                }]
        ]]
      [:div.collapsed.mt-24 {:id id}              ;.collapsed if start collapsed
       content
       ]]]))

(defn munson-new
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        filters @(rf/subscribe [:param :universal [:filters]])
        data @(rf/subscribe [:data :universal {:feature feature :filters filters :dim dim}])]
    [:section.query-builder-section
     [:div.container
      [:div.query-builder-content

       ;; Filter LH side panel
       [:div.filters-view
        [autocomplete/ui]               ;TEMP

        [:div.query-builder-content-headline [:h1 "Query Builder"]]
        [:div.dataset-selection
         [:h3.mb-30.font-bold "Compare across"]
         [:div.dataset-tags
          (for [odim (keys dims)]
            [dim-selector odim dim])
          ]]
        [:div.divider.mt-30.mb-30]
        [filter-ui]]

       ;; Main section
       [:div.pt-20
        [:p.query-builder-section-subheadline
         "Explore multiomic features of glioma tumor microenvironment."]
        [:div.selected-filter-wrapper

         ;; Compare dim panel
         [collapse-panel :dim
          (if-let [dim (get-in dims [@(rf/subscribe [:param :universal :dim]) :label])]
            (str "Selected category: " dim)
              "←  Select a category to compare across")
          [:div#selectedFilterView
           [filter-view]
           [:div.divider.mb-24.mt-24]
           ]
         ]

         ;; Heatmap
         [collapse-panel :heatmap "Sample Distribution Matrix"
          [:img {:src "../assets/images/graph-frame.png"}]
          ]

         ;; Feature selection
         [collapse-panel :feature "Feature Selection"
          (if dim
            [:div {:style {:width "500px"}} ;TODO
             (when-not feature
               [universal/feature-second-warning])
             [fui/ui]]
            [universal/dim-first-warning])]

         ;; Visualization
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
