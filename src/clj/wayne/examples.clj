(ns wayne.examples
  (:require [wayne.bigquery :as bq]
            [oz.core :as oz]
            [org.candelbio.multitool.core :as u]))

(defn data
  [query]
  (->> (bq/query "pici-internal" query)
       (map (fn [x] (update x :feature_value (fn [v] (if (= v "NA") nil (u/coerce-numeric v))))))))

(def q1 "SELECT ROI, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'Stanford' 
and final_diagnosis = 'GBM' 
and feature_type = 'intensity' 
and cell_meta_cluster_final = 'Myeloid_CD14'
and feature_variable = 'CD86' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
")

(def q2 "SELECT ROI, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'Stanford' 
and final_diagnosis = 'GBM' 
and feature_type = 'immune_cell_ratios' 
and feature_variable = 'Tcell_CD4_over_Macrophage_CD163_plus_Tcell_CD4_prop' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
")

(def q3 "SELECT immunotherapy, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'UCSF' 
and feature_type = 'immune_cell_ratios' 
and feature_variable = 'Myeloid_CD11b_over_Myeloid_CD11b_plus_B7H3_func_prop'
")

(def q1-variables
   "SELECT distinct feature_variable FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'Stanford' 
and final_diagnosis = 'GBM' 
and feature_type = 'intensity' 
and cell_meta_cluster_final = 'Myeloid_CD14'
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')")

(defn q1-p
  [feature]
  (format "SELECT ROI, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'Stanford' 
and final_diagnosis = 'GBM' 
and feature_type = 'intensity' 
and cell_meta_cluster_final = 'Myeloid_CD14'
and feature_variable = '%s' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
" feature))


(defn q2-p
  [feature]
  (format "SELECT ROI, feature_value FROM `pici-internal.bruce_external.feature_table` 
where 
site = 'Stanford' 
and final_diagnosis = 'GBM' 
and feature_variable = '%s' 
and ROI IN ('INFILTRATING_TUMOR', 'SOLID_TUMOR')
" feature))

(defonce ex1-data (data q1))

;;; Not necessary
#_
(def ex1-data-filtered (filter :feature_value ex1-data))

;;; TODO rotate to vertical, 
;;; dimension: ROI, immunotherapy...
(defn violin
  [data dim]                            ;TODO dim → better
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
        "transform" [{"type" "filter", "expr" ~(format "datum.%s === parent.%s" dim dim)}]}],
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
          }}
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
    "description" "A violin plot example showing distributions for pengiun body mass."}
  )


;;; YES actually got this to output a violin plot, albeit not a very good one
(defn ex1
  []
  (oz/view! (violin ex1-data) :port 1801 :mode :vega))

;;; TODO doesn't really work, the scales need adjusting
(defn ex1p
  [feature]
  (oz/view! (violin (data (q1-p feature))) :port 1801 :mode :vega))
     
(defn ex2
  []
  (oz/view! (violin (data q2)) :port 1801 :mode :vega))


(defn show-violin
  [data dim]                            ;TODO dim could be inferred
  (oz/view! (violin data dim) :port 1801 :mode :vega))


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
