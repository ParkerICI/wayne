(ns wayne.frontend.munson
  (:require
   [re-frame.core :as rf]
   [com.hyperphor.way.web-utils :as wu]
   com.hyperphor.way.feeds
   [com.hyperphor.way.modal :as modal]
   [com.hyperphor.way.ui.init :as init]
   [wayne.frontend.visualization :as viz]
   [wayne.frontend.sample-dist :as smatrix]
   [wayne.frontend.feature-select :as fui]
   [org.candelbio.multitool.core :as u]
   [com.hyperphor.way.api :as api]
   [wayne.data-defs :as dd]
   [wayne.frontend.examples :as examples]
   [wayne.frontend.utils :as wwu]
   ))

(def dims dd/dims)

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
                   (rf/dispatch [:open-filter-pane dim])
                   (rf/dispatch [:open-collapse-panel :feature])
                   (rf/dispatch [:open-collapse-panel :viz])
                   )}
     (when icon
       [:img.icon {:src (str "../assets/icons/" icon), :alt text :height "18px"}])
     text]))

(defn filter-values-ui
  [dim collapsed?]
  (let [all-values (get-in dims [dim :values])
        feature @(rf/subscribe [:param :universal [:feature]])
        filters @(rf/subscribe [:param :universal [:filters]])
        ;; TODO this ends up accumulating a lot in the db, I don't think its fatal but
        ;; TODO also filter needs to be cleaned/regularized for matching
        in-values @(rf/subscribe [:data [:populate dim] {:dim dim :feature feature :filters filters}])
        ] 
    [:div.accordian-panel {:class (when collapsed? "collapsed")}
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

(defn clear-all-filters-button
  []
  (when-not (empty? @(rf/subscribe [:param :universal [:filters]]))
    [:button.btn.btn-sm.btn-secondary.mx-2
     {:style {:height :fit-content}
      :on-click #(do (rf/dispatch [:set-param :universal :filters {}])
                     (rf/dispatch [:set-param :heatmap :filter {}])
                     (rf/dispatch [:set-param :universal :feature nil]))
      }
     "Clear All"]))

(rf/reg-sub
 :filter-pane
 (fn [db _]
   (:filter-pane db)))

(rf/reg-event-db
 :toggle-filter-pane
 (fn [db [_ pane]]
   (update db :filter-pane #(if (= % pane) nil pane))))

(rf/reg-event-db
 :open-filter-pane
 (fn [db [_ pane]]
   (assoc db :filter-pane pane)))

(defn filter-ui
  []
  (let [open-pane @(rf/subscribe [:filter-pane])]
    [:div.filters
     [:h3.mb-30.font-bold.filter-subheader
      "FILTER"
      [wwu/info "Select molecular and clinical criteria to filter the data for visualization"]
      [clear-all-filters-button]
      ;; temp
      #_
      [:button.btn.btn-sm.btn-secondary.mx-2
       {:on-click #(rf/dispatch [:remember-example])}
       "Remember"]
      #_
      [:button.btn.btn-sm.btn-secondary.mx-2
       {:on-click #(rf/dispatch [:recall-example])}
       "Recall"]
      ]
     [:div.filter-list
      (for [dim (keys dims)]
        [:div.accordian.accordian-collapsed 
         [:div.accordian-title {:on-click #(rf/dispatch [:toggle-filter-pane dim])}
          [:h3 (get-in dims [dim :label])
           (when-let [info-text (get-in dims [dim :info])]
             [wwu/info info-text])]
          [:img {:src "../assets/icons/minus-grey.svg"}]]
         [filter-values-ui dim (not (= dim open-pane))]
         ])]]))

(defn filter-view
  []
  [:fieldset.selected-filter-list
   {:style {:height "auto"}}
   [:legend "Filters"]
   [clear-all-filters-button]
   (u/mapf (fn [[col vals]]
             (let [in-vals (u/mapf (fn [[v in]]
                                     (if in v))
                                   vals)]
               ;; TODO :span not quite right
               [:span
                (wu/humanize col)
                (map (fn [v]
                       [:div.tag {:on-click #(rf/dispatch [:set-param :universal [:filters col v] false])}
                        (wu/humanize v)
                        [:img {:src "../assets/icons/close.svg"}]])
                     in-vals)]))
           @(rf/subscribe [:param :universal [:filters]]))
   ])


(rf/reg-sub
 :collapse-panel-open?
 (fn [db [_ id]]
   (get-in db [:collapse-panel id])))

(rf/reg-event-db
 :toggle-collapse-panel
 (fn [db [_ id]]
   (update-in db [:collapse-panel id] not)))

(rf/reg-event-db
 :open-collapse-panel
 (fn [db [_ id]]
   (assoc-in db [:collapse-panel id] true)))

(defn collapse-panel
  [id title content]
  [:div.featured-view.relative
   [:div.features-view
    [:div.features-view-header {:on-click #(rf/dispatch [:toggle-collapse-panel id])}
     [:div.flex.align-center.flex-row.gap-8 [:h3 title]]
     [:div.flex.gap-16
      [:img#toggleSelectForm {:src "../assets/icons/merge-horizontal-grey.svg"
                              }]
      ]]
    [:div.mt-24 {:class (when-not @(rf/subscribe [:collapse-panel-open? id]) "collapsed")}
     content
     ]]])

(defn dim-first-warning
  []
  [:div.mt-4
   [:span.alert.alert-info.text-nowrap "← First select a dimension to compare ←"]])

(defn feature-second-warning
  []
  [:div.my-3
   [:span.alert.alert-info.text-nowrap "↓  Next select a feature below ↓"]])

;;; TODO with size adjust, use for LH buttons
(defn dim-display
  [dim]
  (let [icon (get-in dims [dim :icon])
        text (get-in dims [dim :label])]
    [:span [:img.icon {:src (str "../assets/icons/" icon),
                       :alt text
                       :height "40px"
                       :style {:vertical-align :middle
                               :margin-left "5px"
                               :margin-right "5px"
}}]
     text]))


(defn munson-new
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        feature_type @(rf/subscribe [:param :features :feature-feature_type])
        filters @(rf/subscribe [:param :universal [:filters]])
        data @(rf/subscribe [:data :universal {:feature feature :feature_type feature_type  :filters filters :dim dim}])]
    [:section.query-builder-section
     [:div.container
      [:div.query-builder-content

       ;; Filter LH side panel
       [:div.filters-view
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
       [:div.pt-6
        [:p.query-builder-section-subheadline
         "Explore multiomic features of glioma tumor microenvironment."]
        [:div.selected-filter-wrapper

         [collapse-panel :examples "Examples"
          [examples/example-chooser]
          ]

         ;; Compare dim panel
         [collapse-panel :dim
          (if dim
            [:span "Selected category: " [dim-display dim]]
            "←  Select a category to compare across")
          [:div#selectedFilterView
           [filter-view]
           [:div.divider.mb-24.mt-24]
           ]
          ]

         ;; Heatmap
         [collapse-panel :heatmap "Sample Distribution Matrix"
          [smatrix/sample-matrix]
          ]

         ;; Feature selection
         [collapse-panel :feature "Feature Selection"
          (if dim
            [:div {:style {:width "500px"}} ;TODO
             ;; Turned this off, it flashes distractingly, and we always have a feature selected
             #_                             
             (when-not feature
               [feature-second-warning])
             [fui/ui]]
            [dim-first-warning])]

         ;; Visualization
         [collapse-panel :viz
          [:span "Visualization "
           (when @(rf/subscribe [:loading?])
             (wu/spinner 1))]
          [:div#visualization.visualization-container
           [:div
            [viz/visualization dim feature data]
            #_
            [:div.no-data.text-center
             [:img {:src "../assets/icons/empty-box.svg"}]
             [:h3 "No Data Found"]
             [:p "Enter or adjust your filters to see data."]]]]]]]]]])
  )

;;; Omit zeros on marker_intensity (as per Hadeesha 5/28/2024).
;;; Might make more sense to do this on server, but easier here.
;;; TODO Obsolete in new feature scheme, ask Hadeesha if still needed
#_
(defn trim-zeros?
  ([]
   (= "marker_intensity" @(rf/subscribe [:param :features :feature-type])))
  ;; this version can be called in more places
  ([db]
   (= "marker_intensity" (get-in db [:params :features :feature-type])))
   )

#_
(defmethod feeds/postload :universal
  [db _ data]
  (if (trim-zeros? db)
    (update-in db
               [:data [:universal]]
               (fn [rows]
                 (remove (fn [row] (= 0 (:feature_value row)))
                         rows)))
    db))


(defn app-ui
  []
  [:div
   [modal/modal]
   [munson-new]
   ])

(defn ^:export init
  []
  (init/init app-ui nil)
  )

;;; TODO this patches a Way method, should be done there 
(rf/reg-event-db
 :fetch
 (fn [db [_ data-id params]]
   (api/api-get
    "/data"
    {:params (merge (get-in db [:params data-id]) 
                    (assoc params :data-id data-id)) 
     :handler #(rf/dispatch [:com.hyperphor.way.feeds/loaded data-id params %])
     :error-handler #(rf/dispatch [:data-error data-id %1]) ;Override standard error handler
     })
   (-> db
       (assoc :loading? true)
       (assoc-in [:data-status data-id] :fetching)
       (assoc-in [:data-params data-id] params))))
