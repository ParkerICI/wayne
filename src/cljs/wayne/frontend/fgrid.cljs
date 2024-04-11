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

(defn ui
  [& [dim]]
  [:div.p-5
   [v/vega-lite-view (spec dim) []]])
