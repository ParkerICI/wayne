(ns wayne.frontend.dendrogram
  (:require [re-frame.core :as rf]
            ["vega-embed" :as ve]
            [clojure.string :as str]
            [wayne.frontend.data :as data]
            [way.web-utils :as wu]
            [way.vega :as v]
            [way.tabs :as tabs]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            )
  )

;;; https://vega.github.io/vega/examples/tree-layout/

;;; Original
#_
{:description "An example of Cartesian layouts for a node-link diagram of hierarchical data.",
 :width 600,
 :scales
 [{:name "color",
   :type "linear",
   :range {:scheme "magma"},
   :domain {:data "tree", :field "depth"},
   :zero true}],
 :padding 5,
 :marks
 [{:type "path",
   :from {:data "links"},
   :encode {:update {:path {:field "path"}, :stroke {:value "#ccc"}}}}
  {:type "symbol",
   :from {:data "tree"},
   :encode
   {:enter {:size {:value 100}, :stroke {:value "#fff"}},
    :update {:x {:field "x"}, :y {:field "y"}, :fill {:scale "color", :field "depth"}}}}
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
 :$schema "https://vega.github.io/schema/vega/v5.json",
 :signals
 [{:name "labels", :value true, :bind {:input "checkbox"}}
  {:name "layout", :value "tidy", :bind {:input "radio", :options ["tidy" "cluster"]}}
  {:name "links",
   :value "diagonal",
   :bind {:input "select", :options ["line" "curve" "diagonal" "orthogonal"]}}
  {:name "separation", :value false, :bind {:input "checkbox"}}],
 :height 1600,
 :data
 [{:name "tree",
   :url "data/flare.json",
   :transform
   [{:type "stratify", :key "id", :parentKey "parent"}
    {:type "tree",
     :method {:signal "layout"},
     :size [{:signal "height"} {:signal "width - 100"}],
     :separation {:signal "separation"},
     :as ["y" "x" "depth" "children"]}]}
  {:name "links",
   :source "tree",
   :transform
   [{:type "treelinks"} {:type "linkpath", :orient "horizontal", :shape {:signal "links"}}]}]}


;;; Trimmed and customized

;; TODO 


(def spec
  {:description "An example of Cartesian layouts for a node-link diagram of hierarchical data.",
   :width 600 :height 600
   :$schema "https://vega.github.io/schema/vega/v5.json",

   ;; These are trying to get layout right
   ;; :autosize "pad"
   :layout {:align "each"
            :columns 2
            ;;:padding 70,     
            ;; :bounds "flush",
            ;; :align "none"
            }
   ;; :config {:axisY {:minExtent 30}}

   :scales
   [#_{:name "color",
       :type "linear",
       :range {:scheme "magma"},
       :domain {:data "tree", :field "depth"},
       :zero true}],

   :signals
   [{:name "labels", :value true, :bind {:input "checkbox"}}
    #_ {:name "layout", :value "tidy", :bind {:input "radio", :options ["tidy" "cluster"]}}
    #_ {:name "links",
        :value "diagonal",
        :bind {:input "select", :options ["line" "curve" "diagonal" "orthogonal"]}}
    {:name "separation", :value false, :bind {:input "checkbox"}}
    {:name "hm_width" :value 400}
    {:name "hm_height" :value 400}
    {:name "dend_width" :value 200}]
   :padding 5,
   :marks
   [

    {:type :group                       ;Empty quadrant
     :style :cell
     :encode {:update {:width {:signal "dend_width"},
                       :height {:signal "dend_width"} ;??? why does this affect the OTHER group???
                       }
              }
     }


    ;; V tree
    {:type "group"
     :style "cell"
     :data
     [{:name "tree",
                                        ; :url "https://vega.github.io/vega/data/flare.json"
       :url "dend.json"
       :transform
       [{:type "stratify", :key "id", :parentKey "parent"}
        {:type "tree",
         :method "cluster",
         :size [{:signal "hm_width"} {:signal "dend_width - 100"}],
         :separation {:signal "separation"},
         :as ["x" "y" "depth" "children"]}]}
      {:name "links",
       :source "tree",
       :transform
       [{:type "treelinks"}
        {:type "linkpath", :orient "vertical", :shape "orthogonal"}]}]
     :encode {:update {:width {:signal "hm_width"}, ;This is what finally got the layout semi-sane
                       :height {:signal "dend_width"} ;??? why does this affect the OTHER group???
                       }
              }
     :marks [{:type "path",
              :from {:data "links"},
              :encode {:update {:path {:field "path"}, :stroke {:value "#ccc"}}}}
             #_{:type "symbol",
                :from {:data "tree"},
                :encode
                {:enter {:size {:value 100}, :stroke {:value "#fff"}},
                 :update {:x {:field "x"}, :y {:field "y"}, :fill {:scale "color", :field "depth"}}}}
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
       :url "dend.json",
       :transform
       [{:type "stratify", :key "id", :parentKey "parent"}
        {:type "tree",
         :method "cluster",
         :size [ {:signal "hm_height"} {:signal "dend_width"}],
         :separation {:signal "separation"},
         :as ["y" "x" "depth" "children"]}]}
      {:name "links",
       :source "tree",
       :transform
       [{:type "treelinks"}
        {:type "linkpath", :orient "horizontal", :shape "orthogonal"}]}]
     :encode {:update {:width {:signal "dend_width"}
                       :height {:signal "hm_height"}
                       }
              }
     :marks [{:type "path",
              :from {:data "links"},
              :encode {:update {:path {:field "path"}, :stroke {:value "#ccc"}}}}
             #_{:type "symbol",
                :from {:data "tree"},
                :encode
                {:enter {:size {:value 100}, :stroke {:value "#fff"}},
                 :update {:x {:field "x"}, :y {:field "y"}, :fill {:scale "color", :field "depth"}}}}
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
                       :width #_ {:signal "hm_width"}  {:value 80}, ;TODO compute from data size
                       :height #_ {:signal "hm_height"} {:value 140}
                       }
              },
     
     :scales
     [{:name "x" :type "band" :domain {:data "hm" :field "recurrence1"} :range {:step 20} }
      {:name "y" :type "band" :domain {:data "hm" :field "feature_variable"} :range {:step 20}}
      {:name "color"
       :type "linear"
       :range {:scheme "BlueOrange"}
       :domain {:data "hm", :field "feature_value"},
;       "reverse": {"signal": "reverse"},
;      "zero": false, "nice": true
       }]

     :axes
     [{:orient :right :scale :y :title "feature"} 
      {:orient :bottom :scale :x :title "recurrence" :labelAngle 90 :labelAlign "left"}]

     :marks
     [{:type "rect"
       :from {:data "hm"}
       :encode
       {:enter
        {:y {:field "feature_variable" :scale "y"}
         :x {:field "recurrence1" :scale "x"}
         :width {:value 19} :height {:value 19}
         :fill {:field "feature_value" :scale "color"}

                                        ;:width {:value 15}
                                        ;:height {:value 5}
         }}}
      ]
     }
    ]}) 

(defn dendrogram
  []  
  [v/vega-view spec []])

(defn ui
  []
  [dendrogram])
