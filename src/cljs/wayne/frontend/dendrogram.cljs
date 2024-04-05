(ns wayne.frontend.dendrogram
  (:require [way.vega :as v]
            )
  )

;;; Based on https://vega.github.io/vega/examples/tree-layout/

;;; TODO row/col annotations w colors and scales, see examples

(def features1 ["EGFR_func_over_all_tumor_prop"
                "GM2_GD2_func_over_all_tumor_prop"
                "GPC2_func_over_all_tumor_prop"
                "VISTA_func_over_all_tumor_prop"
                "HER2_func_over_all_tumor_prop"
                "B7H3_func_over_all_tumor_prop"
                "NG2_func_over_all_tumor_prop"
                ])


(def recurrence1
  ["Primary"
   "Recurrence"
   "Normal_brain"])

(def spec
  {:description "A clustered heatmap with side-dendrograms",
   :width 600 :height 600
   :$schema "https://vega.github.io/schema/vega/v5.json",
   :layout {:align "each"
            :columns 2}
   :scales []
   :signals
   [{:name "labels", :value true, :bind {:input "checkbox"}}
    {:name "hm_width" :value 60}
    {:name "hm_height" :value 140}      ;TODO derive from data
    {:name "dend_width" :value 40}]
   :padding 5,
   :marks
   [
    {:type :group                       ;Empty quadrant
     :style :cell
     :encode {:update {:width {:signal "dend_width"},
                       :height {:signal "dend_width"} ;??? why does this affect the OTHER group???
                       :strokeWidth {:value 0}
                       }
              }
     }

    ;; V tree
    {:type "group"
     :style "cell"
     :data
     [{:name "tree",
       :url "dend2.json"
       :transform
       [{:type "stratify", :key "id", :parentKey "parent"}
        {:type "tree",
         :method "cluster",
         :size [{:signal "hm_width"} {:signal "dend_width"}],
         :as ["x" "y" "depth" "children"]}]}
      {:name "links",
       :source "tree",
       :transform
       [{:type "treelinks"}
        {:type "linkpath", :orient "vertical", :shape "orthogonal"}]}]
     :encode {:update {:width {:signal "hm_width"}, ;This is what finally got the layout semi-sane
                       :height {:signal "dend_width"} ;??? why does this affect the OTHER group???
                       :strokeWidth {:value 0}
                       }
              }
     :marks [{:type "path",
              :from {:data "links"},
              :encode {:update {:path {:field "path"}, :stroke {:value "#666"}}}}
             #_ 
             {:type "text",
              :from {:data "tree"},
              :encode
              {:enter {:text {:field "name"}, :fontSize {:value 9}, :baseline {:value "middle"}},
               :update
               {:x {:field "x"},
                :y {:field "y"},
                :dy {:signal "datum.children ? -7 : 7"},
                :align {:signal "datum.children ? 'right' : 'left'"},
                :angle {:value 90}
                :opacity {:signal "labels ? 1 : 0"}}}}],
     }

    ;; H tree
    {:type "group"
     :name "htree"
     :style "cell"
     :data
     [{:name "tree",
       :url "dend1.json",
       :transform
       [{:type "stratify", :key "id", :parentKey "parent"}
        {:type "tree",
         :method "cluster",
         :size [ {:signal "hm_height"} {:signal "dend_width"}],
         :as ["y" "x" "depth" "children"]}]}
      {:name "links",
       :source "tree",
       :transform
       [{:type "treelinks"}
        {:type "linkpath", :orient "horizontal", :shape "orthogonal"}]} ] ;diagonal is kind of interesting but not standard
     :encode {:update {:width {:signal "dend_width"}
                       :height {:signal "hm_height"}
                       :strokeWidth {:value 0} }}
     :marks [{:type "path",
              :from {:data "links"},
              :encode {:update {:path {:field "path"}, :stroke {:value "#666"}}}}
             #_
             {:type "text",
              :from {:data "tree"},
              :encode
              {:enter {:text {:field "name"}, :fontSize {:value 9}, :baseline {:value "middle"}},
               :update
               {:x {:field "x"},
                :y {:field "y"},
                :dx {:signal "datum.children ? -7 : 7"},
                :align {:signal "datum.children ? 'right' : 'left'"},
                :opacity {:signal "labels ? 1 : 0"}}}}],
     }

    ;; actual hmap 
    {:type "group"
     :name "heatmap"
     :style "cell"
     :data [{:name "hm",
             :url "hm2.json"}]
     :encode {
              :update {
                       :width {:signal "hm_width"}
                       :height {:signal "hm_height"}
                       }
              },
     
     :scales
     ;; TODO note the feature values inserted here. Alt would be to have them in the data somehow
     [{:name "x" :type "band" :domain recurrence1 :range {:step 20} }
      {:name "y" :type "band" :domain features1 :range {:step 20}}
      {:name "color"
       :type "linear"
       :range {:scheme "BlueOrange"}
       :domain {:data "hm", :field "feature_value"},
       }]

     :axes
     [{:orient :right :scale :y :title "feature"} 
      {:orient :bottom :scale :x :title "recurrence" :labelAngle 90 :labelAlign "left"}]

     :legends
     [{:fill :color
       :type :gradient
       :title "Median feature value"
       :titleOrient "bottom"
       :gradientLength {:signal "hm_height"}
       }]

     :marks
     [{:type "rect"
       :from {:data "hm"}
       :encode
       {:enter
        {:y {:field "feature_variable" :scale "y"}
         :x {:field "recurrence1" :scale "x"}
         :width {:value 19} :height {:value 19}
         :fill {:field "feature_value" :scale "color"}
         }}}
      ]
     }
    ]}) 

(defn dendrogram
  []  
  [v/vega-view spec []])

(defn ui
  []
  [:div.p-5
   [dendrogram]])
