(ns org.parkerici.wayne.frontend.x.sites
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.aggrid :as ag]
            [com.hyperphor.way.tabs :as tab]
            [com.hyperphor.way.vega :as v]
            )
  )

;;; Color schemes
;;; https://vega.github.io/vega/docs/schemes/

(defn pie-spec
  [data]
  {:data {:values data}
   :repeat ["patients", "samples"]
   :spec {:title {:text {:repeat "repeat"}} ;TODO argh, can't figure out how to do this simple task. My guess: you can do it with expressions, somehow.
          :mark {:type "arc", :tooltip {:content "data"} },
          :encoding
          {:color {:field "site", :type "nominal"
                   :scale {:scheme "tableau20"}},
           :theta {:field {:repeat "repeat"} :type "quantitative"
                   ;;nope :axis { :labels true :labelExpr "datum"}
                   }
           ;; At least puts some text in, but not the right kind...
           ;;:column {:field {:repeat "repeat"}}
           ;; :column {:field "argh"}
           ;; :column {:field {:repeat "repeat"}}
           ;; :header {:title "nope"}
           }
          }
   })
          
;;; Bar chart, more informative and has proper titles. 
(defn bar-spec
  [data]
  {:data {:values data}
   :transform [{:calculate "'/site/' + datum.site", ;TODO make this work or leave it out
                :as "url"
                }],
   :repeat ["patients", "samples"] ; , "features"
   :spec {:mark {:type "bar", :tooltip true },
          :encoding
          {:y {:field "site", :type "nominal" :axis {:title false}},
           :x {:field {:repeat "repeat"} :type "quantitative"}
           :color {:field "site", :type "nominal" ;Color optional
                   :scale {:scheme "tableau20"}
                   :legend false}       ;don't really need it with the labels
           }}
   })

(defn sites
  []
  [:div
   [:h3 "Sites"]
   ;; Debug
   (let [sites @(rf/subscribe [:data :sites {:fake :it}])]
     [:div
      [v/vega-lite-view (bar-spec sites) sites]
      [v/vega-lite-view (pie-spec sites) sites] ;TODO labels don't show due to Vega issue
      [ag/ag-table 
       sites
       ]
      ])])

(defmethod tab/set-tab [:tab :sites]
  [id tab db]
  (rf/dispatch [:fetch-once :sites]))

