(ns wayne.frontend.cohort
  (:require [re-frame.core :as rf]
            [way.tabs :as tab]
            [way.vega :as v]
            [wayne.frontend.fgrid :as fgrid]
            [way.web-utils :as wu]
            [org.candelbio.multitool.core :as u]
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
          {:y {:field "final_diagnosis", :type "nominal" :axis {:title false}},
           :x {:field {:repeat "repeat"} :type "quantitative"}
           :color {:field "final_diagnosis", :type "nominal" ;Color optional
                   :scale {:scheme "tableau20"}
                   :legend false}       ;don't really need it with the labels
           :href {:field "url"}
           }}
   })


(defmethod tab/set-tab [:tab :about]
  [id tab db]
  (rf/dispatch [:fetch-once :cohort]))

(defn ui
  []
  [:div
   [:h3 "About the cohort and analysis"]
   ;; Debug
   (let [cohorts @(rf/subscribe [:data :cohort])]
     [:div
      [v/vega-lite-view (bar-spec cohorts) cohorts]
      [fgrid/ui]
      ])]
  )
