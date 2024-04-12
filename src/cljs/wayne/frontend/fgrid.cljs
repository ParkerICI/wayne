(ns wayne.frontend.fgrid
  (:require [way.vega :as v]
            [org.candelbio.multitool.core :as u]
            )
  )

(defn spec
  [dim]
  {:data {:url "filter-grid.js"}
   :transform (if dim
                [{:filter (u/expand-template "test(/^{dim}/, datum.dim1) && !(test(/^{dim}/, datum.dim2))"
                                             {"dim" (name dim)})}]
                [])
   :mark {:type "rect", :tooltip {:content "data"} },
   :encoding
   {:x {:field "dim2" :type "nominal" :axis {:grid true :bandPosition 0 :orient :top} :title false}
    :y {:field "dim1" :type "nominal" :axis {:grid true :bandPosition 0} :title false}
    :color {:field "count" :type "quantitative" :scale {:domain [0 100]}}
    ;; TODO damn it this does not work, apparently adding event handlers is rocket science to vega lite. 
    ;; In reality, this should populate the filter appropriately. 
    ;; Going back to Vega-linte 5.15.1 did not fix, was hoping
    :href {:value "javascript:alert(\"foo\")"} ; 
    }
   })

;;; Full Vega spec, compiled from above with some hand additions
;;; TODO generate spec via compile + merge
(defn vspec
  [dim]
  {:legends [{:fill "color", :gradientLength {:signal "clamp(height, 64, 200)"}, :title "count"}],
   :config {},
   :axes
   [{:labels false,
     :scale "x",
     :grid true,
     :gridScale "y",
     :orient "top",
     :minExtent 0,
     :aria false,
     :maxExtent 0,
     :bandPosition 0,
     :ticks false,
     :domain false,
     :zindex 1}
    {:labels false,
     :scale "y",
     :grid true,
     :gridScale "x",
     :orient "left",
     :minExtent 0,
     :aria false,
     :maxExtent 0,
     :bandPosition 0,
     :ticks false,
     :domain false,
     :zindex 1}
    {:scale "x",
     :orient "top",
     :grid false,
     :bandPosition 0,
     :labelAlign "left",
     :labelAngle 270,
     :labelBaseline "middle",
     :zindex 1}
    {:scale "y", :orient "left", :grid false, :bandPosition 0, :zindex 1}],
   :background "white",
   :scales
   [{:name "x",
     :type "band",
     :domain {:data "source_0", :field "dim2", :sort true},
     :range {:step {:signal "x_step"}},
     :paddingInner 0,
     :paddingOuter 0}
    {:name "y",
     :type "band",
     :domain {:data "source_0", :field "dim1", :sort true},
     :range {:step {:signal "y_step"}},
     :paddingInner 0,
     :paddingOuter 0}
    {:name "color",
     :type "linear",
     :domain [0 100],
     :range "heatmap",
     :interpolate "hcl",
     :zero true}],
   :style "cell",
   :padding 5,
   :marks
   [{:name "marks",
     :type "rect",
     :style ["rect"],
     :from {:data "source_0"},
     :encode
     {:update
      {:y {:scale "y", :field "dim1"},
       :description
       {:signal
        "\"dim2: \" + (isValid(datum[\"dim2\"]) ? datum[\"dim2\"] : \"\"+datum[\"dim2\"]) + \"; dim1: \" + (isValid(datum[\"dim1\"]) ? datum[\"dim1\"] : \"\"+datum[\"dim1\"]) + \"; count: \" + (format(datum[\"count\"], \"\"))"},
       :fill {:scale "color", :field "count"},
       :width {:signal "max(0.25, bandwidth('x'))"},
       :cursor {:value "pointer"},
       :x {:scale "x", :field "dim2"},
       :href {:value "javascript:alert(\"foo\")"},
       :tooltip {:signal "datum"},
       :height {:signal "max(0.25, bandwidth('y'))"}}}}
    ],
   :$schema "https://vega.github.io/schema/vega/v5.json",
   :signals
   [{:name "x_step", :value 20}
    {:name "width", :update "bandspace(domain('x').length, 0, 0) * x_step"}
    {:name "y_step", :value 20}
    {:name "height", :update "bandspace(domain('y').length, 0, 0) * y_step"}
    #_
    {:name "hover",
     :value nil,
     :on [
          {:events "rect:mouseover", :update "datum.dim1"},
          {:events "mousedown!", :update "null"}
          ]
     }
    {:name "click"
     :on [{:events "rect:mousedown" :update "datum.dim2"}]}
    ],
   :data
   [{:name "source_0",
     :url "filter-grid.js",
     :format {:type "json"},
     :transform
     [{:type "filter", :expr (if dim
                               (u/expand-template "test(/^{dim}/, datum.dim1) && !(test(/^{dim}/, datum.dim2))"
                                                  {"dim" (name dim)})
                               "true")}
      {:type "filter", :expr "isValid(datum[\"count\"]) && isFinite(+datum[\"count\"])"}]}]})

(defn lite-ui
  [& [dim]]
  [:div.p-5
   [v/vega-lite-view (spec dim) []]])

(defn ui
  [& [dim]]
  [:div.p-5
   [v/vega-view (vspec dim) []]])
