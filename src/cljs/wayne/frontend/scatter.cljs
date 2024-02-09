(ns wayne.frontend.scatter
  (:require [re-frame.core :as rf]
            ["vega-embed" :as ve]
            [wayne.frontend.data :as data] ;temp
            [wayne.way.tabs :as tab]
            [wayne.way.api :as api]
            [wayne.way.web-utils :as wu]
            [reagent.dom]
            [clojure.string :as str]
            )
  )

;;; Misnamed, this is not a scatterplot
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

(defn do-vega
  [spec]
  (js/module$node_modules$vega_embed$build$vega_embed.embed "#vis1" (clj->js spec)))

(rf/reg-event-db
 :loaded
 (fn [db [_ data]]
   (let [spec (case (get-in db [:active-tab :tab])
                :dotplot dot-spec
                :barchart bar-spec)]
     (do-vega (spec data))
     (assoc db :loading? false))))

(rf/reg-event-db
 :fetch-scatter
 (fn [db _]
   (api/ajax-get "/api/v2/data0" {:params (:params db)
                                  :handler #(rf/dispatch [:loaded %])
                                  })
   (assoc db :loading? true)))

(defn plot
  []
  [:div
   #_ [:button {:on-click #(do-vega (spec))} "Fill"]
   [:nav.navbar.navbar-expand-lg
       [:ul.navbar-nav.mr-auto
        [:li.nav-item
         (wu/select-widget
          :site
          nil                                 ;todo value
          #(rf/dispatch [:set-param :site %])
          data/sites
          "Site")]
        [:li.nav-item
         (wu/select-widget
          :feature
          nil                                 ;todo value
          #(rf/dispatch [:set-param :feature %])
          data/features
          "Feature")]]]
   [:div#vis1]
   ])

(defmethod tab/set-tab [:tab :dotplot]
  [db]
  (do-vega {}))

(defmethod tab/set-tab [:tab :barchart]
  [db]
  (do-vega {}))


                                        ;Since we are multiplexing, clear on tab switch

