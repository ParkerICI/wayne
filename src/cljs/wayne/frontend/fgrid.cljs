(ns wayne.frontend.fgrid
  (:require [way.vega :as v]
            )
  )

(def spec
  {:data {:url "filter-grid.js"}
   :mark {:type "rect", :tooltip {:content "data"} },
   :encoding
   {:x {:field "dim1" :type "nominal" :axis {:grid true :bandPosition 0}}
    :y {:field "dim2" :type "nominal" :axis {:grid true :bandPosition 0}}
    :color {:field "count" :type "quantitative" :scale {:domain [0 100]}}
    }
   })

(defn ui
  []
  [:div.p-5
   [v/vega-lite-view spec []]])
