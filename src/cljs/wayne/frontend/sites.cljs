(ns wayne.frontend.sites
  (:require [re-frame.core :as rf]
            [way.aggrid :as ag]
            [way.tabs :as tab]
            [way.vega :as v]
            [way.data :as data]
            )
  )

(defn pie-spec
  [data]
  {:mark {:type "arc", :tooltip {:content "data"}, :clip true :filled true},
   :data {:values data}
   :transform [{:calculate "'/site/' + datum.site", ;TODO make this work or leave it out
                :as "url"
                }],
   :title "Patients"
   :encoding
   {:color {:field "site", :type "nominal"},
    :theta {:field "patients" :type "quantitative"}
    :tooltip {:field "site"}
    :label {:field "patients" :type "quantitative"}
    :href {:field "url"}
    }
   })

(defn sites
  []
  [:div
   [:h3 "Sites"]
   ;; Debug
   [:button {:on-click #(v/do-vega (pie-spec @(rf/subscribe [:data :sites])) "#viz2")} "Redraw"]

   (let [sites @(rf/subscribe [:data :sites])]
     [:div
      [:div#viz2]
      [ag/ag-table 
       :sites
       (keys (first sites))
       sites
       {}
       ]
      ])])

(defmethod tab/set-tab [:tab :sites]
  [id tab db]
  (rf/dispatch [:fetch-once :sites]))

(defmethod data/loaded :sites
  [id data db]
  (v/do-vega (pie-spec data) "#viz2"))
