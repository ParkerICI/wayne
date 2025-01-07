(ns wayne.frontend.x.cohort
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.vega :as v]
            [wayne.frontend.x.fgrid :as fgrid]
            [com.hyperphor.way.aggrid :as ag]
            )
  )

(defn bar-spec
  [data]
  {:data {:values data}
   :transform [{:calculate "'/site/' + datum.site", ;TODO make this work or leave it out
                :as "url"
                }],
   :repeat ["patients", "samples"]  ; , "features"
   :spec {:mark {:type "bar", :tooltip true },
          ;; :header {:title "foo"}
          :encoding
          {:y {:field "Tumor_Diagnosis", :type "nominal" :axis {:title false}},
           :x {:field {:repeat "repeat"} :type "quantitative"}
           :color {:field "Tumor_Diagnosis", :type "nominal" ;Color optional
                   :scale {:scheme "tableau20"}
                   :legend false}       ;don't really need it with the labels
           :href {:field "url"}
           }}
   })

(defn ui
  []
  [:div
   [:h3 "About the cohort and analysis"]
   ;; Debug
   (let [cohorts @(rf/subscribe [:data :cohort {:fake :it}])]
     [:div
      [v/vega-lite-view (bar-spec cohorts) cohorts]
      [ag/ag-table 
       cohorts
       ]
      [fgrid/ui]
      ])]
  )
