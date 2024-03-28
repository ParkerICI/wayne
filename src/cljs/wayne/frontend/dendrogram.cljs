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
   :$schema "https://vega.github.io/schema/vega/v5.json",
   :signals
   [{:name "labels", :value true, :bind {:input "checkbox"}}
    #_ {:name "layout", :value "tidy", :bind {:input "radio", :options ["tidy" "cluster"]}}
    #_ {:name "links",
        :value "diagonal",
        :bind {:input "select", :options ["line" "curve" "diagonal" "orthogonal"]}}
    {:name "separation", :value false, :bind {:input "checkbox"}}],
   :height 1600,
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
      {:type "linkpath", :orient "horizontal", :shape "orthogonal"}]}]})

(defn dendrogram
  []  
  [v/vega-view spec []])

(defn ui
  []
  [:div "Hey loser"
   [dendrogram]])
