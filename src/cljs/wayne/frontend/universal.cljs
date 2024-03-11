(ns wayne.frontend.universal
  (:require [re-frame.core :as rf]
            ["vega-embed" :as ve]
            [wayne.frontend.data :as data]
            [way.web-utils :as wu]
            [way.vega :as v]
            [way.tabs :as tab]
            [reagent.dom]
            )
  )

(defn violin
  [data dim]
  (let [dim (name dim)]
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
      "domain" {"data" "source", "field" ~dim}}
     {"name" "xscale",
      "type" "linear",
      "range" "width",
      "round" true,
      "domain" {"data" "source", "field" "feature_value"},
      "zero" false,
      "nice" true}
     {"name" "hscale",
      "type" "linear",
      "range" [0 {"signal" "plotWidth"}],
      "domain" {"data" "density", "field" "density"}}
     {"name" "color",
      "type" "ordinal",
      "domain" {"data" "source", "field" ~dim},
      "range" "category"}],
    "axes"
    [{"orient" "bottom", "scale" "xscale", "zindex" 1}
     {"orient" "left", "scale" "layout", "tickCount" 5, "zindex" 1}],
    "signals"
    [{"name" "plotWidth", "value" 160}  ;controls fatness of violins
     {"name" "height", "update" "(plotWidth + 10) * 3"}
     {"name" "trim", "value" true, "bind" {"input" "checkbox"}}
     {"name" "bandwidth", "value" 0, "bind" {"input" "range", "min" 0, "max" 0.00002, "step" 0.000001}}], ;Note: very sensitive, was hard to find these values
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


;;; Note sure how to do "stage"
(def grouping-features [:site :final_diagnosis :who_grade :cohort :ROI :recurrence])

;;; See radio-buttong groups https://getbootstrap.com/docs/5.3/components/button-group/#checkbox-and-radio-button-groups
(defn dim-chooser
  [f]
  [:div.col
   (for [feature grouping-features
         :let [label (name feature)]]
    [:div.form-check
     [:input.form-check-input {:type :radio
                               :name "feature"
                               :id label
                               :on-click #(f feature)}]
     [:label.form-check-label {:for label} (name feature)]])])

(defn values
  []
  (let [feature @(rf/subscribe [:param :universal-meta :feature])
        values (sort (get data/values-c feature))] ;Ah looking in the data here
  [:div.col
   (for [value values
         :let [id (str "feature" value)]]
     [:div.form-check
      [:input.form-check-input
       {:type :checkbox
        :id id
        :on-change (fn [e]
                     (rf/dispatch
                      [:set-param :universal-meta [:values feature value] (-> e .-target .-checked)]
                      ))
        }]
      [:label.form-check-label {:for id} value]])
   ]))
  
(defn ui
  []
  (let [data @(rf/subscribe [:data :universal])
        dim @(rf/subscribe [:param :universal :dim])] 
    (prn :dim dim :data (count data))
    [:div
     [:div.row
      [:h4 "Filter"]
      [dim-chooser
       #(rf/dispatch [:set-param :universal-meta :feature %])]
      [values]]

     [:div.row
      [:h4 "Compare"]
      [dim-chooser
       #(rf/dispatch [:set-param :universal :dim %])]
      ]
     [:div.row
      [:h4 "Feature Selection"]
      ;; TODO hierarchy as in Stanford design, and/or limit with filters
      (wu/select-widget                 
       :feature
       nil                                 ;todo value
       #(rf/dispatch [:set-param :universal :feature %])
       data/features
       "Feature")
      ]
     [:div.row
      ;; Feature
      [:h4 "Visualization"]
      [v/vega-view (violin data dim) data]
      ]]))
