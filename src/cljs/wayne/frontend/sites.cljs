(ns wayne.frontend.sites
  (:require [re-frame.core :as rf]
            [way.aggrid :as ag]
            [way.tabs :as tab]
            ["vega-embed" :as ve]
            )
  )

(defn pie-spec
  [data]
  {:mark {:type "arc", :tooltip {:content "data"}, :clip true :filled true},
   :data {:values data}
   :encoding
   {:color {:field "site", :type "nominal"},
    :theta {:field "patients" :type "quantitative"}
    }})

(defn sites
  []
  [:div
   [:h3 "Sites"]
   (let [sites @(rf/subscribe [:data :sites])]
     [:div
      [ag/ag-table 
       :sites
       (keys (first sites))
       sites
       {}
       ]
      [:div#viz2]])])

(defmethod tab/set-tab [:tab :sites]
  [id tab db]
  (rf/dispatch [:fetch-once :sites]))
