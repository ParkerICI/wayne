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

(def spec
  {:description "An example of Cartesian layouts for a node-link diagram of hierarchical data.",
   :width 400,
   :$schema "https://vega.github.io/schema/vega/v5.json",
   :height 400,

   ;; Therese are trying to get layout right
   ;; :autosize "pad"
   :layout {                            ;yes having an empty element here makes a differnece
            ;;:padding 70,     
            ;; :bounds "flush",
            ;; :align "none"
            },
   ;; :config {:axisY {:minExtent 30}}
   ;; END

   :scales
   [{:name "color",
     :type "linear",
     :range {:scheme "magma"},
     :domain {:data "tree", :field "depth"},
     :zero true}],
   :data
   [{:name "tree",
     :url "https://vega.github.io/vega/data/flare.json",
     :transform
     [{:type "stratify", :key "id", :parentKey "parent"}
      {:type "tree",
       :method "cluster",
       :size [{:signal "height"} {:signal "width - 100"}],
       :separation {:signal "separation"},
       :as ["y" "x" "depth" "children"]}]}
    {:name "links",
     :source "tree",
     :transform
     [{:type "treelinks"}
      {:type "linkpath", :orient "horizontal", :shape "orthogonal"}]}]
   :signals
   [{:name "labels", :value true, :bind {:input "checkbox"}}
    #_ {:name "layout", :value "tidy", :bind {:input "radio", :options ["tidy" "cluster"]}}
    #_ {:name "links",
        :value "diagonal",
        :bind {:input "select", :options ["line" "curve" "diagonal" "orthogonal"]}}
    {:name "separation", :value false, :bind {:input "checkbox"}}]
   :padding 5,
   :marks
   [{:type "group"
     :style "cell"
     :title {:text "nothingness" :frame "group"}
     :encode {:update {:width {:signal "height"}, ;This is what finally got the layout semi-sane
                       :height {:signal "height"}
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

    ;; Second view
    {:type "group"
     :name "heatmap"
     :style "cell"
     :title {:text "your ad here" :frame "group"}
;     :width 400
;     :height 400
     :encode {
              :update {
                       :width {:signal "height"},
                       :height {:signal "height"}
        }
      },

    :marks
     [{:type "rect"
       :from  {:data "tree"}
        :encode
        {:enter
           {:y {:field "x"}
            :x {:field "y"}
            :width {:value 15}
            :height {:value 5}}}}
      ]
     }]}) 

(defn dendrogram
  []  
  [v/vega-view spec []])

(defn ui
  []
  [dendrogram])
