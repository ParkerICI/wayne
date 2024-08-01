(ns wayne.frontend.munson
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [hyperphor.way.ui.init :as init]
   [wayne.frontend.universal :as universal]
   [wayne.frontend.feature-select :as fui]
   [org.candelbio.multitool.core :as u]
   ))


;;; This is universal.cljs, but adapted to run in Munson website.


(def dims
  {:final_diagnosis "Final Diagnosis"
   :who_grade "WHO grade"
   :ROI "ROI"
   :recurrence "Recurrence"
   :idh_status "IDH Status"
   :treatment "Treatment"
   })

(defn dim-selector
  [dim icon-url active-dim]
  (let [text (get dims dim)]
    [:div.dataset-tag 
     {;; :data-icon data-icon
      :class (when (= dim active-dim) "dataset-tag-active")
      :on-click #(do
                   (rf/dispatch [:set-param :universal :dim dim])
                   (rf/dispatch [:set-param :heatmap2 :dim dim]))}
     [:img.icon {:src icon-url, :alt text}]
     text]))

(defn munson-new
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        data @(rf/subscribe [:data [:universal {:feature feature}]])]
  [:section.query-builder-section
   [:div.container
    [:div.query-builder-content
     [:div.filters-view
      [:div.query-builder-content-headline [:h1 "Query Builder"]]
      [:div.dataset-selection
       [:h3.mb-30.font-bold "Compare across"]
       [:div.dataset-tags
        [dim-selector :final_diagnosis "../assets/icons/diagnosis-icon.svg" dim]
        [dim-selector :who_grade "../assets/icons/question-icon.svg" dim]
        [dim-selector :ROI "../assets/icons/roi-icon.svg" dim]
        [dim-selector :recurrence "../assets/icons/recurrence-icon.svg" dim]
        [dim-selector :idh_status "../assets/icons/file-chart-icon.svg" dim]
        [dim-selector :treatment "../assets/icons/treatment-icon.svg" dim]
        ]]
      [:div.divider.mt-30.mb-30]
      [:div.filters
       [:h3.mb-30.font-bold.filter-subheader "Filter"]
       [:div.filter-list
        [:div.accordian.accordian-collapsed
         [:div.accordian-title
          [:h3 "Final diagnosis"]
          [:img {:src "../assets/icons/minus-grey.svg"}]]
         [:div.accordian-panel
          [:label.custom-checkbox
           [:input {:type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "Astrocytoma"]
          [:label.custom-checkbox.custom-checkbox-disabled
           [:input {:disabled "disabled", :type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "Ganglioglioma"]
          [:label.custom-checkbox
           [:input {:type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "Oligodendroglioma"]
          [:label.custom-checkbox
           [:input {:type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "pGBM"]]]
        [:div.accordian [:h3 "Who grade"] [:img {:src "../assets/icons/plus-grey.svg"}]]
        [:div.accordian [:h3 "ROI"] [:img {:src "../assets/icons/plus-grey.svg"}]]
        [:div.accordian [:h3 "Recurrence"] [:img {:src "../assets/icons/plus-grey.svg"}]]
        [:div.accordian [:h3 "Treatment"] [:img {:src "../assets/icons/plus-grey.svg"}]]
        [:div.accordian [:h3 "IDH Status"] [:img {:src "../assets/icons/plus-grey.svg"}]]]]]
     [:div.pt-20
      [:p.query-builder-section-subheadline
       "Run sensitivity analyses to explore insights in cancer research,\n                from clinical"
       [:br {:clear "none"}]
       "trials to innovative therapies."]
      [:div.selected-filter-wrapper
       [:div.selected-filter-view
        [:div.filter-headline
         [:div.flex.align-center.justify-content-between.gap-8 [:h3#active-tag-text (get dims @(rf/subscribe [:param :universal :dim]))]]
         [:div.flex.align-center.justify-content-between.gap-16
          [:img#toggleSelectedFilterView
           {:src "../assets/icons/merge-horizontal-grey.svg", :alt "navbar-cllapse"}]
          [:img {:src "../assets/icons/external-link.svg", :alt "external-link"}]]]
        [:div#selectedFilterView
         [:div.selected-filter-list
          [:div.tag "Final Diagnosis" [:img {:src "../assets/icons/close.svg"}]]
          [:div.tag "Astrocytoma" [:img {:src "../assets/icons/close.svg"}]]
          [:div.tag "Ganglioglima" [:img {:src "../assets/icons/close.svg"}]]
          [:button.clear-all-button {:type "submit"} "Clear All"]]
         [:div.divider.mb-24.mt-24]
         ;; Static heatmap
         #_ [:img {:src "../assets/images/graph-frame.png"}]]]
       [:div.featured-view.relative
        [:div.features-view
         [:div.features-view-header
          [:div.flex.align-center.flex-row.gap-8 [:h3 "Feature Selection"]]
          (if dim
            [:div {:style {:width "500px"}} ;TODO
             (when-not feature
               [universal/feature-second-warning])
             [fui/ui]]
            [universal/dim-first-warning])
          [:div.flex.gap-16
           [:img#toggleSelectForm {:src "../assets/icons/merge-horizontal-grey.svg"}]
           [:img {:src "../assets/icons/download.svg"}]]]
         [:div.select-form-group.mt-24
          [:div.custom-select-wrapper
           [:div.custom-select
            [:select
             [:option {:selected "selected", :value "EphA2"} "EphA2"]
             [:option {:value "Option 1"} "Option 1"]
             [:option {:value "Option 2"} "Option 2"]
             [:option {:value "Option 3"} "Option 3"]]]]]]]
       [:div.data-view.relative
        [:div.visualization-header
         [:div.flex.gap-8.align-center [:h3 "Visualization"]]
         [:div.flex.gap-16
          [:img#toggleVisualization {:src "../assets/icons/merge-horizontal-grey.svg"}]
          [:img {:src "../assets/icons/download.svg"}]]]
        [:div#visualization.visualization-container
         [:div
          [universal/visualization dim feature data]
          #_
          [:div.no-data.text-center
           [:img {:src "../assets/icons/empty-box.svg"}]
           [:h3 "No Data Found"]
           [:p "Enter or adjust your filters to see data."]]]]]]]]]])
  )

#_
(defn munson-old
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        data @(rf/subscribe [:data [:universal {:feature feature}]])]

    [:div.query-builder-content
     [:div.filters-view
      [:div.dataset-selection
       [:h3.mb-30 "Compare across"]
       [:div.dataset-tags

        [dim-selector :final_diagnosis "../assets/icons/diagnosis-icon.svg" dim]
        [dim-selector :who_grade "../assets/icons/question-icon.svg" dim]
        [dim-selector :ROI "../assets/icons/roi-icon.svg" dim]
        [dim-selector :recurrence "../assets/icons/recurrence-icon.svg" dim]
        [dim-selector :idh_status "../assets/icons/file-chart-icon.svg" dim]
        [dim-selector :treatment "../assets/icons/treatment-icon.svg" dim]

        ]]
      [:div.divider.mt-30.mb-30]
      [:div.filters
       [:h3.mb-30 "Filter"]
       [:div.filter-list
        [:div.accordian.accordian-collapsed
         [:div.accordian-title [:h3 "Final diagnosis"] [:img {:src "../assets/icons/minus.svg"}]]
         [:div.accordian-panel
          [:label.custom-checkbox
           [:input {:type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "Astrocytoma"]
          [:label.custom-checkbox.custom-checkbox-disabled
           [:input {:disabled "disabled", :type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "Ganglioglioma"]
          [:label.custom-checkbox
           [:input {:type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "Oligodendroglioma"]
          [:label.custom-checkbox
           [:input {:type "checkbox", :hidden "hidden", :_ "_"}]
           [:span.checkmark]
           "pGBM"]]]
        [:div.accordian [:h3 "Who grade"] [:img {:src "../assets/icons/plus.svg"}]]
        [:div.accordian [:h3 "ROI"] [:img {:src "../assets/icons/plus.svg"}]]
        [:div.accordian [:h3 "Recurrence"] [:img {:src "../assets/icons/plus.svg"}]]
        [:div.accordian [:h3 "Treatment"] [:img {:src "../assets/icons/plus.svg"}]]
        [:div.accordian [:h3 "IDH Status"] [:img {:src "../assets/icons/plus.svg"}]]]]]
     [:div
      [:div.selected-filter-view
       [:div.filter-headline
        [:div.flex.align-center.justify-content-between.gap-8
         [:img {:src "../assets/icons/task-list.svg", :alt "task-list"}]
         [:h3#active-tag-text (get dims @(rf/subscribe [:param :universal :dim]))]] 
        [:div.flex.align-center.justify-content-between.gap-16
         [:img {:src "../assets/icons/layout-navbar-expand-blue.svg", :alt "navbar-cllapse"}]
         [:img {:src "../assets/icons/external-link.svg", :alt "external-link"}]]]
       [:div.selected-filter-list
        [:div.tag "Final Diagnosis" [:img {:src "../assets/icons/close.svg"}]]
        [:div.tag "Astrocytoma" [:img {:src "../assets/icons/close.svg"}]]
        [:div.tag "Ganglioglima" [:img {:src "../assets/icons/close.svg"}]]
        [:button.clear-all-button {:type "submit"} "Clear All"]]]
      [:div.data-view.relative
       [:div.features-view
        [:div.features-view-header
         [:div.flex.align-center.flex-row.gap-8
          [:img {:src "../assets/icons/chart-infographic.svg"}]
          [:h3 "Feature Selection"]]
         (if dim
           [:div {:style {:width "500px"}} ;TODO
            (when-not feature
              [universal/feature-second-warning])
            [fui/ui]]
           [universal/dim-first-warning])
         ]
        [:div.divider.mb-24.mt-24]
        [:div.visualization-header
         [:div.flex.gap-8.align-center
          [:img {:src "../assets/icons/graph.svg"}]
          [:h3 "Visualization"]]
         [:div.flex.gap-12
          [:img {:src "../assets/icons/download.svg"}]
          [:img {:src "../assets/icons/layout-navbar-collapse-grey.svg"}]]]
        [universal/visualization dim feature data]]]]
     ]))


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
