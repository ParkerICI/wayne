(ns wayne.frontend.dotplot
  (:require [re-frame.core :as rf]
            [wayne.frontend.data :as data] ;temp
            [hyperphor.way.vega :as v]
            [hyperphor.way.download :as download]
            [hyperphor.way.web-utils :as wu]
            [reagent.dom]
            )
  )

;;; Data Laziness, make this have feature selector and data server

(defn clean-zeros
  [data]
  (map (fn [row] (update row :feature_value #(if (or (= % 0) (= % "0")) nil %))) data))

;;; Dotplot
(defn dot-spec
  [data]
  {:mark {:type "point", :tooltip {:content "data"}, :clip true :filled true},
   :data {:values (clean-zeros data)}
   :params [{:name "sample",
             :select {:type :point :fields ["sample_id"] :on "pointerdown"},
             :bind "legend"

             }],
   :encoding
   {"color" {:field "sample_id", :type "nominal"},
    #_ :opacity #_  {:condition {:param "sample" :value 1} :value 0.1}
    :size  {:condition {:param "sample" :value 100} :value 20}
    :shape {:field "ROI" :type "nominal"}
    "y" {:field "cell_meta_cluster_final", :type "nominal"},
    "x"  {:field "feature_value"
          :type "quantitative"
          :scale  {:type "log"
                   ; :domainMax "10e-05"
                  },
          }},
   :height 700, 
   :width 700, 
   })

(defn plot
  []
  (let [data @(rf/subscribe [:data :dotplot])]
    [:div
     [:nav.navbar.navbar-expand-sm
      [:div.container-fluid
       [:div.collapse.navbar-collapse
        [:ul.navbar-nav
         [:li.nav-item.mx-2
          (wu/select-widget
           :site
           nil                                 ;todo value
           #(rf/dispatch [:set-param :dotplot :site %])
           data/sites
           "Site")]
         [:li.nav-item.mx-2
          (wu/select-widget
           :feature
           nil                                 ;todo value
           #(rf/dispatch [:set-param :dotplot :feature %])
           data/features
           "Feature")]
         [:li.nav-item.mx-2
          [:form
           (download/button data "wayne-export.tsv")
           ]]]]]]
     [v/vega-lite-view (dot-spec data) data]
     ]))



   

