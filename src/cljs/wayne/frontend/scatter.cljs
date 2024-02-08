(ns wayne.frontend.scatter
  (:require ["vega-embed" :as ve]
            [wayne.frontend.data :as data] ;temp
            [wayne.way.tabs :as tab]
            [reagent.dom]
            [clojure.string :as str]
            )
  )

;;; Misnamed, this is not a scatterplot
;;; Data Laziness, make this have feature selector and data server

(defn clean-zeros
  [data]
  (map (fn [row] (update row :feature_value #(if (= % "0") nil %))) data))

(defn spec
  []
  {:mark {:type "point", :tooltip {:content "data"}, :clip true},
   :data {:values (clean-zeros data/cd4)}
   :encoding
   {"color" {:field "sample_id", :type "nominal"},
    "y" {:field "cell_meta_cluster_final", :type "nominal"},
    "x"  {:field "feature_value"
          :type "quantitative"
          :scale  {:type "log"
                   ; :domainMax "10e-05"
                  },
          }},
   :heightkig 700, 
   :width 700, 
   })

(defn do-vega
  [spec]
  #_(js/vegaEmbed "#vis" (clj->js a-spec))
  (js/module$node_modules$vega_embed$build$vega_embed.embed "#vis" (clj->js spec)))

(defn plot
  []
  [:div
   [:button {:on-click #(do-vega (spec))} "Fill"]
   #_ [:nav.navbar.navbar-expand-lg
       [:ul.navbar-nav.mr-auto
        [:li.nav-item
         (wu/select-widget
          :site
          nil                                 ;todo value
          #(rf/dispatch [:set-param :site %])
          sites
          "Site")]
        [:li.nav-item
         (wu/select-widget
          :feature
          nil                                 ;todo value
          #(rf/dispatch [:set-param :feature %])
          features
          "Feature")]]]
   [:div#vis]
   ])

(defmethod tab/set-tab [:tab :scatter]
  [db]
  (do-vega (spec)))
