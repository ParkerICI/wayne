(ns wayne.examples
  (:require [wayne.bigquery :as bq]
            [oz.core :as oz]
            [org.candelbio.multitool.core :as u]))

(defn data
  [query]
  (->> (bq/query "pici-internal" query)
  ;; TODO cleaup values
      (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v))))))))

(def q1 "SELECT ROI, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'Stanford' 
and final_diagnosis = 'GBM' 
and feature_type = 'intensity' 
and cell_meta_cluster_final = 'Myeloid_CD14'
and feature_variable = 'CD86' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')")

(defonce ex1-data (data q1))

(def ex1-data-filtered (filter :feature_value ex1-data))

(def violin
  `{"width" 500,
    "config" {"axisBand" {"bandPosition" 1, "tickExtra" true, "tickOffset" 0}},
    "padding" 5,
    "marks"
    [{"type" "group",
      "from" {"facet" {"data" "density", "name" "violin", "groupby" "ROI"}},
      "encode"
      {"enter"
       {"yc" {"scale" "layout", "field" "ROI", "band" 0.5},
        "height" {"signal" "plotWidth"},
        "width" {"signal" "width"}}},
      "data"
      [{"name" "summary",
        "source" "stats",
        "transform" [{"type" "filter", "expr" "datum.ROI === parent.ROI"}]}],
      "marks"
      [{"type" "area",
        "from" {"data" "violin"},
        "encode"
        {"enter" {"fill" {"scale" "color", "field" {"parent" "ROI"}}},
         "update"
         {"x" {"scale" "xscale", "field" "value"},
          "yc" {"signal" "plotWidth / 2"},
          "height" {"scale" "hscale", "field" "density"}}}}
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
      "domain" {"data" "source", "field" "ROI"}}
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
      "domain" {"data" "source", "field" "ROI"},
      "range" "category"}],
    "axes"
    [{"orient" "bottom", "scale" "xscale", "zindex" 1}
     {"orient" "left", "scale" "layout", "tickCount" 5, "zindex" 1}],
    "signals"
    [{"name" "plotWidth", "value" 60}
     {"name" "height", "update" "(plotWidth + 10) * 3"}
     {"name" "trim", "value" true, "bind" {"input" "checkbox"}}
     {"name" "bandwidth", "value" 0, "bind" {"input" "range", "min" 0, "max" 0.00002, "step" 0.000001}}], ;Note: very sensitive, was hard to find these values
    "$schema" "https://vega.github.io/schema/vega/v5.json",
    "data"
    [{"name" "source",
      "values" ~ex1-data-filtered}
     {"name" "density",
      "source" "source",
      "transform"
      [{"type" "kde",
        "field" "feature_value",
        "groupby" ["ROI"],
        "bandwidth" {"signal" "bandwidth"}
        "extent" {"signal" "trim ? null : [2000, 6500]"}
        }]} 
     {"name" "stats",
      "source" "source",
      "transform"
      [{"type" "aggregate",
        "groupby" ["ROI"],
        "fields" ["feature_value" "feature_value" "feature_value"], ;??? do not understand why we need to repeat this three times, but it is important
        "ops" ["q1" "median" "q3"],
        "as" ["q1" "median" "q3"]}]}],
    "description" "A violin plot example showing distributions for pengiun body mass."}
  )


;;; YES actually got this to output a violin plot, albeit not a very good one
(defn ex1
  []
  (oz/view! violin :port 1801 :mode :vega))
     

(def box
  `{
    "$schema" "https://vega.github.io/schema/vega-lite/v5.json",
    "data" {"values" ~ex1-data},
    "mark" "boxplot",
    "encoding"
    {"x" {"field" "ROI", "type" "nominal"},
     "color" {"field" "ROI", "type" "nominal", "legend" nil},
     "y" {
          "field" "feature_value",
          "type" "quantitative",
          "scale" {"zero" false}
          }
     }
    })

(defn ex1a
  []
  (oz/view! box :port 1562))

(def dots
  `{
    "$schema" "https://vega.github.io/schema/vega-lite/v5.json",
    "data" {"values" ~ex1-data},
    "mark" "point",
    "encoding"
    {"x" {"field" "ROI", "type" "nominal"},
     "color" {"field" "ROI", "type" "nominal", "legend" nil},
     "y" {
          "field" "feature_value",
          "type" "quantitative",
          }
     }
    })

;;; Yay, finally got some markings to show
(defn ex1b
  []
  (oz/view! dots :port 1561))



;;; Alt: https://github.com/JonyEpsilon/gg4cljn (hasn't changed in 10 years!)
