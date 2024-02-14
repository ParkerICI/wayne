(ns wayne.frontend.barchart
  (:require [re-frame.core :as rf]
            [wayne.frontend.data :as data] ;temp
            [way.tabs :as tab]
            [way.data :as wdata]
            [way.vega :as v]
            [way.api :as api]
            [way.web-utils :as wu]
            [reagent.dom]
            [clojure.string :as str]
            )
  )

;;; Data Laziness, make this have feature selector and data server

(defn clean-zeros
  [data]
  (map (fn [row] (update row :feature_value #(if (or (= % 0) (= % "0")) nil %))) data))

;;; Stacked bars from Enjun
(defn bar-spec
  [data]
  {:mark {:type "bar", :tooltip {:content "data"}},
   :data {:values (clean-zeros data)}
   :encoding
   {"y" {:field "sample_id", :type "nominal"},
    "x" {:aggregate "count"
         :type "quantitative"}
    "color" {:field "cell_meta_cluster_final", :type "nominal"}
    }
   :width 700, 
   })


(defn plot
  []
  (let [data @(rf/subscribe [:data :barchart])]
    [:div
     [:nav.navbar.navbar-expand-sm
      [:div.container-fluid
       [:div.collapse.navbar-collapse
        [:ul.navbar-nav
         [:li.nav-item.mx-2
          (wu/select-widget
           :site
           nil                                 ;todo value
           #(rf/dispatch [:set-param :barchart :site %])
           data/sites
           "Site")]
         [:li.nav-item.mx-2
          (wu/select-widget
           :feature
           nil                                 ;todo value
           #(rf/dispatch [:set-param :barchart :feature %])
           data/features
           "Feature")]
         [:li.nav-item.mx-2
          [:form
           (wu/download-button @(rf/subscribe [:data :barchart]) "wayne-export.tsv")
           ]]]]]]
     [v/vega-lite-view (bar-spec data) data]   
     ]))




   

