(ns wayne.frontend.universal
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [wayne.frontend.signup :as signup]
            [way.web-utils :as wu]
            [way.vega :as v]
            [way.tabs :as tabs]
            way.data                    ;for sub handlers
            [way.download :as download]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.fgrid :as fgrid]
            [wayne.frontend.feature-select :as fui]
            [wayne.frontend.dendrogram :as dendro]
            )
  )

(defn interpret-scale
  [scale]
  (case scale
    "log2" {:type "log" :base 2}
    "log10" {:type "log" :base 10}
     {:type scale}))

(defn violin
  [data dim feature]
  (let [dim (name dim)
        scale (interpret-scale @(rf/subscribe [:param :features :scale]))] ;TODO wee fui/ref below
    `{"width" 700,
      "config" {"axisBand" {"bandPosition" 1, "tickExtra" true, "tickOffset" 0}},
      "padding" 5,
      "marks"
      [{"type" "group",
        "from" {"facet" {"data" "density", "name" "violin", "groupby" ~dim}},
        "encode"
        {"enter"
         {"yc" {"scale" "layout", "field" ~dim, "band" 0.5},
          "height" {"signal" "plotWidth"},
          "width" {"signal" "width"}}},
        "data"
        [{"name" "summary",
          "source" "stats",
          "transform" [{"type" "filter", "expr" ~(wu/js-format "datum.%s === parent.%s" dim dim)}]}],
        "marks"
        [{"type" "area",
          "from" {"data" "violin"},
          "encode"
          {"enter" {"fill" {"scale" "color", "field" {"parent" ~dim}}},
           "update"
           {"x" {"scale" "xscale", "field" "value"},
            "yc" {"signal" "plotWidth / 2"},
            "height" {"scale" "hscale", "field" "density"}}}}
         {"type" "symbol"
          "from" {"data" "source"}
          "encode"
          {"enter" {"fill" "black" "y" {"value" 0}},
           "update"
           {"x" {"scale" "xscale", "field" "feature_value"},
            "yc" {"signal" "plotWidth / 2 + 80*(random() - 0.5)"}, ;should scale with fatness
            "size" {"value" 25},
            "shape" {"value" "circle"},
            "strokeWidth" {"value" 1},
            "stroke" {"value" "#000000"}
            "fill" {"value" "#000000"}
            "opacity" {"value" "0.3"}
            }
           }
          }
         {"type" "rect",
          "from" {"data" "summary"},
          "encode"
          {"enter" {"fill" {"value" "black"}, "height" {"value" 2}},
           "update"
           {"x" {"scale" "xscale", "field" "q1"},
            "x2" {"scale" "xscale", "field" "q3"},
            "yc" {"signal" "plotWidth / 2"}}}}
         {"type" "rect",
          "from" {"data" "summary"},
          "encode"
          {"enter" {"fill" {"value" "black"}, "width" {"value" 2}, "height" {"value" 8}},
           "update" {"x" {"scale" "xscale", "field" "median"}, "yc" {"signal" "plotWidth / 2"}}}}]}],
      "scales"
      [{"name" "layout",
        "type" "band",
        "range" "height",
        "domain" {"data" "source", "field" ~dim}
        "paddingOuter" 0.5}
       ~(merge
         {"name" "xscale",
          "range" "width",
          "round" true,
          "domain" {"data" "source", "field" "feature_value"},
                                        ;       "zero" false,
          "nice" true
          }
         scale)
       {"name" "hscale",
        "type" "linear",
        "range" [0 {"signal" "plotWidth"}],
        "domain" {"data" "density", "field" "density"}}
       {"name" "color",
        "type" "ordinal",
        "domain" {"data" "source", "field" ~dim},
        "range" "category"}],
      "axes"
      [{"orient" "bottom", "scale" "xscale", "zindex" 1 :title ~(wu/humanize feature)} ;TODO want metacluster in this
       {"orient" "left", "scale" "layout", "tickCount" 5, "zindex" 1}],
      "signals"
      [{"name" "plotWidth", "value" 160}  ;controls fatness of violins
       {"name" "height", "update" "(plotWidth + 10) * 3"}
       {"name" "trim", "value" true, #_ "bind" #_ {"input" "checkbox"}}
       ;; TODO this didn't work, so going out of Vega
       #_ {"name" "xscales", "value" "linear" "bind"  {"input" "select" "options" ["linear" "log10" "log2" "sqrt"]}}
       {"name" "bandwidth", "value" 0, #_ "bind" #_ {"input" "range", "min" 0, "max" 0.00002, "step" 0.000001}}], ;Note: very sensitive, was hard to find these values
      "$schema" "https://vega.github.io/schema/vega/v5.json",
      "data"
      [{"name" "source",
        "values" ~data}
       {"name" "density",
        "source" "source",
        "transform"
        [{"type" "kde",
          "field" "feature_value",
          "groupby" [~dim],
          "bandwidth" {"signal" "bandwidth"}
          "extent" {"signal" "trim ? null : [0.0003, 0.0005]"} ;TODO not sure what this does or how to adjust it propery
          }]} 
       {"name" "stats",
        "source" "source",
        "transform"
        [{"type" "aggregate",
          "groupby" [~dim],
          "fields" ["feature_value" "feature_value" "feature_value"], ;??? do not understand why we need to repeat this three times, but it is important
          "ops" ["q1" "median" "q3"],
          "as" ["q1" "median" "q3"]}]}],
      "description" "A violin plot example showing distributions for pengiun body mass."}))

(defn boxplot
  [data dim]
  (let [scale (interpret-scale @(rf/subscribe [:param :features :scale]))]
    {
     :$schema "https://vega.github.io/schema/vega-lite/v5.json",
     :data {:values data}
     :mark {:type "boxplot" :tooltip true}, ; :extent "min-max"
     :encoding {:y {:field "feature_value", :type "quantitative"
                    :scale scale},
                :x {:field dim :type "nominal"}
                :color {:field dim :type "nominal", :legend nil}
                }
     }))

(def grouping-features [:final_diagnosis :who_grade :ROI :recurrence
                        :treatment :idh_status])

;;; Just build this in, generated by wayne.data/generate-filters
(def filter-features
  '{:ROI
    ("INFILTRATING_TUMOR" "NORMAL_BRAIN" "SOLID_INFILTRATING_TUMOR" "SOLID_TUMOR" "TUMOR" "other"),
    :treatment
    ("CHOP_brain_cptac_2020"
     "CHOP_openpbta"
     "CHOP_pbta_all"
     "CHOP_unknown"
     "CoH_control"
     "CoH_neoadjuvant"
     "Stanford_unknown"
     "UCLA_control"
     "UCLA_neoadjuvant_nonresp"
     "UCLA_neoadjuvant_resp"
     "UCSF_0"
     "UCSF_lys_control"
     "UCSF_neoadjuvant_SPORE_CD27"
     "UCSF_neoadjuvant_SPORE_vaccine"
     "UCSF_neoadjuvant_lys_vaccine"
     "UCSF_non_trial_controls"
     "UCSF_pre_trial"
     "UCSF_pxa_group"),
    :who_grade ("2" "3" "4" "NA"),
    :idh_status ("NA" "mutant" "unknown" "wild_type"),
    :recurrence ("no" "unknown" "yes"),
,
    :final_diagnosis
    ("Astrocytoma"
     "Diffuse_midline_glioma"
     "GBM"
     "Ganglioglioma"
     "Glioma"
     "Normal_brain"
     "Oligodendroglioma"
     "PXA"
     "Thalmic_glioma"
     "pGBM"
     "pHGG")})

(defn filter-values-ui
  [dim]
  (let [all-values (get filter-features dim)
        feature @(rf/subscribe [:param :universal [:feature]])
        filters @(rf/subscribe [:param :universal [:filters]])
        ;; TODO this ends up accumulating a lot in the db, I don't think its fatal but
        ;; TODO also filter needs to be cleaned/regularized for matching
        in-values @(rf/subscribe [:data [:universal-pop dim feature filters]])
        ] 
    [:div
     (for [value all-values
           :let [id (str "dim" (name dim) "-" value)
                 checked? (get-in filters [dim value])
                 disabled? (not (contains? in-values value))
                 ]]
       [:div.form-check
        {:key (str "filter-val-" dim value)}
        [:input.form-check-input
         {:type :checkbox
          :key id
          :id id
          :disabled (if disabled? "disabled" "")
          :checked (if checked? "checked" "")
          :on-change (fn [e]
                       (rf/dispatch
                        [:set-param :universal [:filters dim value] (-> e .-target .-checked)])
                       (rf/dispatch
                        [:set-param :heatmap [:filter dim value] (-> e .-target .-checked)]))
          }]
        [:label.form-check-label {:for id :class (when disabled? "text-muted")
                                                   ; text-decoration-line-through
                                  }
         (wu/humanize value) disabled?]])
     ]))

(defn filter-ui
  [compare-dim]
  [:div#filter-accordion.accordion
   (for [dim grouping-features]
     (let [collapse-id (str "collapse" (name dim))
           heading-id (str "heading" (name dim))]
       [:div.accordion-item
        {:key (str "filter-dim-" dim)}
        [:h2.accordion-header {:id heading-id}
         [:button.accordion-button.collapsed {:type "button"
                                              :data-bs-toggle "collapse"
                                              :data-bs-target (str "#" collapse-id)
                                              :aria-expanded "true"
                                              :aria-controls collapse-id
                                              :class (if (= dim compare-dim) "bg-info")
                                              }
          (wu/humanize dim)
          (when (= dim compare-dim) [:i.px-2 "(y-axis)"])]]
        ;; .show
        [:div.accordion-collapse.collapse {:id collapse-id
                                           :aria-labelledby heading-id
                                           :data-bs-parent "#filter-accordion"}
         [:div.accordion-body
          [filter-values-ui dim]
          ]]]
       ))])

;;; See radio-button groups https://getbootstrap.com/docs/5.3/components/button-group/#checkbox-and-radio-button-groups
(defn dim-chooser
  [vname f]
  [:div.col
   (for [feature grouping-features
         :let [label (name feature)]]
    [:div.form-check
     {:key (str "dim-chooser-" feature)}
     [:input.form-check-input {:type :radio
                               :name vname
                               ; TODO :value ...
                               :id label
                               :on-click #(f feature)}]
     [:label.form-check-label {:for label} (wu/humanize feature)]])])

(defn filter-text
  []
  (let [filter @(rf/subscribe [:param :universal [:filters]])]
    [:div.border.rounded.bg-light {:style {:text-wrap "wrap", :text-indent "-10px", :padding-left "15px" :padding-top "5px" :padding-right "5px"}}
     (u/mapf (fn [[col vals]]
               (let [in-vals (u/mapf (fn [[v in]]
                                       (if in v))
                                     vals)]
                 (when-not (empty? in-vals)
                   [:p
                    {:key (str "filter-text-" (name col))}
                    (str (wu/humanize (name col)) ": "
                            (str/join ", "
                                      (map wu/humanize in-vals)))])))
             filter)]))

(defn heatmap
  [dim]
  (let [data @(rf/subscribe [:data :heatmap])]
    [v/vega-lite-view
     {:mark :rect
      :data {:values data}
      :encoding {:y {:field dim :type "nominal"} 
                 :x {:field "feature_variable" :type "nominal"}
                 :color {:field :mean
                         :type "quantitative"
                         :legend {:orient :top}
                         :title "mean feature value"
                         }}   ;Note: mean computed on server
      :config {:axis {:grid true :tickBand :extent}}
      }
     data]))

(rf/reg-event-db
 :vega-click
 (fn [db [_ value]]
   (let [[_ dim val] (re-matches #"(.*): (.*)" value) ;TODO overly linked to particular Vega data in fgrid
         dim (keyword dim)] 
     (rf/dispatch                    
      [:set-param :universal [:filters dim val] (not (get-in db [:params :universal :filters dim val]))]
      )
     #_ ;; alt
     (update-in db [:params :universal :filters dim val] not)
     )
   ))

(defn dim-first-warning
  []
  [:div.mt-4
   [:span.alert.alert-info.text-nowrap "← First select a dimension to compare ←"]])

(defn feature-second-warning
  []
  [:div.my-3
   [:span.alert.alert-info.text-nowrap "↓  Next select a feature below ↓"]])

(defn scale-chooser
  []
  [:span.hstack "Scale: " (fui/select-widget-minimal :scale ["linear" "log10" "log2" "sqrt" "symlog"])])

(defn dendrogram
  [dim]
  (dendro/dendrogram @(rf/subscribe [:data :heatmap])
                     dim
                     :feature_variable
                     :mean))

(defn ui
  []
  (let [dim @(rf/subscribe [:param :universal :dim])
        feature @(rf/subscribe [:param :universal :feature])
        data @(rf/subscribe [:data :universal])] 
    [:div
     [:div.row
      [:div.col-6
       [:h4 "Visualization"]
       (when dim
         [:div
          ;; TODO pluralize
          [:span (str (count data) " rows")]   ; could do this but it is wrong, and hides the actual 0-data case (if (empty? data) "No data" (str (count data) " rows"))
          [:span.ms-2 (signup/with-signup (download/button data (str "bruce-export-" feature ".tsv")))]
          ;; TODO of course you might want to see these together, so tabs are not good design
          [tabs/tabs
           :uviz
           (array-map
            :violin (fn [] [:div
                            [scale-chooser]
                            [v/vega-view (violin data dim feature) data]
                            ])
            :boxplot (fn [] [:div.vstack
                             [scale-chooser]
                             [v/vega-lite-view (boxplot data dim) data]])
            ;; :heatmap (fn [] [heatmap data dim "site"])
            :heatmap (fn [] [heatmap dim])
            :dendrogram (fn [] [dendrogram dim])
            )]])]
      [:div.col-6
       ;; Temp off because it hides dendrogram
       #_ (when dim (fgrid/ui dim))]
      ]

     [:div.row
      [:div.col-2
       [:h4 "Compare"]
       [dim-chooser
        "compare"
        #(do (rf/dispatch [:set-param :universal :dim %])
             (rf/dispatch [:set-param :heatmap :dim %]))]
       ]
      [:div.col-3
       [:h4 "Filter"
        [:span.ms-2 [:button.btn.btn-outline-primary {:on-click #(do (rf/dispatch [:set-param :universal :filters {}])
                                                                     (rf/dispatch [:set-param :heatmap :filter {}])
                                                                     (rf/dispatch [:set-param :universal :feature nil]))} "Clear"]]
        ]
       [filter-text]                    ;TODO tweak
       
       (if dim
         [filter-ui dim]
         [dim-first-warning]) ]
      #_
      [:div.col-2
       [:h4 " "]
       [filter-text]]
      [:div.col-7
       [:h4 "Feature Selection"]
       (if dim
         [:div
          (when-not feature
            [feature-second-warning])
          [fui/ui]]
         [dim-first-warning])
       ]]
     ]
    ))

