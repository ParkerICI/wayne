(ns wayne.frontend.munson
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [wayne.frontend.universal :as universal]
   [wayne.frontend.feature-select :as fui]

   #_ [wayne.frontend.signup :as signup]

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
                   (rf/dispatch [:set-param :heatmap :dim dim]))}
     [:img.icon {:src icon-url, :alt text}]
     text]))

(defn munsom-raw
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        data @(rf/subscribe [:data :universal])]

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
   [munsom-raw]
   #_ [universal/ui]
   ])

(defn ^:dev/after-load mount-root
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (rf/clear-subscription-cache!)
  (let [root (createRoot (gdom/getElement "app"))]
    (.render root (r/as-element [app-ui]))
    )
  )

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize-db])
  (mount-root)
  )
