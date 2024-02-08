(ns wayne.frontend.violins
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            ["vega-embed" :as ve]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.web-utils :as wu]
            [wayne.frontend.api :as api]
            [wayne.way.tabs :as tab]
            [reagent.dom]
            [clojure.string :as str]
            )
  )

(def features
  ["Olig2"
   "CD133"
   "EphA2"
   "CD31"
   "CD47"
   "CD38"
   "HLADR"
   "CD45"
   "CD4"
   "CD8"
   "CD86"
   "PD1"
   "CD14"
   "Ki67"
   "NG2"
   "H3K27me3"
   "ICOS"
   "CD3"
   "H3K27M"
   "LAG3"
   "B7H3"
   "CD11b"
   "GFAP"
   "NeuN"
   "IDO1"
   "IDH1_R132H"
   "TIM3"
   "GM2_GD2"
   "TMEM119"
   "CD70"
   "CD40"
   "Tox"
   "CD141"
   "CD209"
   "EGFR"
   "CD206"
   "FOXP3"
   "Calprotectin"
   "HLA1"
   "EGFRvIII"
   "ApoE"
   "CD123"
   "GLUT1"
   "CD163"
   "Chym_Tryp"
   "GPC2"
   "CD20"
   "CD208"
   "FoxP3"
   "HER2"
   "VISTA"
   "CD68"
   "PDL1"])

(def sites '("CoH" "CHOP" "UCLA" "UCSF" "Stanford"))

;;; → Multitool
(defn intercalate [l1 l2]
  (cond (empty? l1) l2
        (empty? l2) l1
        :else (cons (first l1) (cons (first l2) (intercalate (rest l1) (rest l2))))))


;;; Format for cljs
;;; → Multitool
;;; Not quite right eg if %s is at start or end
(defn js-format
  [s & args]
  (apply str
         (intercalate (str/split s #"%s")
                      args)))



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
        "transform" [{"type" "filter", "expr" ~(js-format "datum.%s === parent.%s" dim dim)}]}],
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
    "description" "A violin plot example showing distributions for pengiun body mass."}
  )

;;; WAY
(defn vega-div
  []
  [:div#vis])

;;; WAY
;;; TODO generalize for multiple vega divs
(defn do-vega
  [spec]
  #_(js/vegaEmbed "#vis" (clj->js a-spec))
  (js/module$node_modules$vega_embed$build$vega_embed.embed "#vis" (clj->js spec)))

(rf/reg-event-db
 ::loaded
 (fn [db [_ data]]
   (do-vega (violin data "ROI"))        ;TODO generalize dim, can be "immunotherapy" (but needs label)
   (assoc db :loading? false)))

(rf/reg-event-db
 ::fetch
 (fn [db _]
   (api/ajax-get "/api/v2/data0" {:params (:params db)
                                  :handler #(rf/dispatch [::loaded %])
                                  })
   (assoc db :loading? true)))
   
(rf/reg-event-db
 :set-param
 (fn [db [_ param value]]
   (prn :set-param param value db)
   (rf/dispatch [::fetch])
   (assoc-in db [:params param] value)))

(defn violins
  []
  [:div
   [:nav.navbar.navbar-expand-lg
    [:ul.navbar-nav.mr-auto
    [:li.nav-item
     (wu/select-widget
      :site
      nil                                 ;todo value
      #(rf/dispatch [:set-param :site %])
      sites
      "Site")]
    [:li.nav-item
     (wu/select-widget
      :feature
      nil                                 ;todo value
      #(rf/dispatch [:set-param :feature %])
      features
      "Feature")]]]
   (vega-div)
   ])

(defmethod tab/set-tab [:tab :violin]
  [db]
  (rf/dispatch [::fetch]))
