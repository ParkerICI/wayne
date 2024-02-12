(ns wayne.frontend.dotplot
  (:require [re-frame.core :as rf]
            [wayne.frontend.data :as data] ;temp
            [way.tabs :as tab]
            [way.data :as wdata]
            [way.vega :as v]
            [way.web-utils :as wu]
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
  [:div
   #_ [:button {:on-click #(do-vega (spec))} "Fill"]
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
         #_ (wu/download-button @(rf/subscribe [:data :dotplot]) "wayne-export.tsv") ; TODO probably broken
         ]]]]]]
   [:div#vis1]
   ])

(defmethod tab/set-tab [:tab :dotplot]
  [id tab db]
  #_ (do-vega {})
  nil)

(defmethod wdata/loaded :dotplot
  [id data db]
  (v/do-vega (dot-spec data) "#vis1"))

   

