(ns wayne.frontend.dendrogram
  (:require [way.vega :as v]
            )
  )

;;; Based on https://vega.github.io/vega/examples/tree-layout/
;;; TODO row/col annotations w colors and scales, see examples

(defn tree
  [cluster-data left?]
  (let [width-signal (if left? "dend_width" "hm_width")
        height-signal (if left? "hm_height" "dend_width")]
    `{:type "group"
      :style "cell"
      :data 
      [
       {:name "links",
        :source ~cluster-data,
        :transform
        [{:type "treelinks"}
         {:type "linkpath",
          :orient ~(if left? "horizontal" "vertical")
          :shape "orthogonal"}]}]
      :encoding {:width {:signal ~width-signal}, 
                 :height {:signal ~height-signal}
                 :strokeWidth {:value 0}
                 }
      :marks [{:type "path",
               :from {:data "links"},
               :encode {:enter
                        {:path {:field "path"},
                         :stroke {:value "#666"}}}}

              #_
              {:type "text",
               :from {:data "tree"},
               :encode              
               {:enter
                {:text {:field "id"},
                 :fontSize {:value 9},
                 :baseline {:value "middle"}},
                :update
                {:x {:field "x"},
                 :y {:field "y"},
                 ;; :dy {:signal "datum.children ? -7 : 7"},
                 ;; :align {:signal "datum.children ? 'right' : 'left'"},
                 :angle {:value 90}
                 :tooltip {:signal "datum"}
                 :opacity {:signal "labels ? 1 : 0"}}}}],
      }))


;;; Generates TWO data specs (for tree and filtered to leaves)
(defn tree-data-spec
  [name url left?]
  `[{:name ~name,
     :url ~url   ; :value s ~cluster-tree
     :transform
     [{:type "stratify", :key "id", :parentKey "parent"}
      {:type "tree",
       :method "cluster",
       :size [{:signal ~(if left? "hm_height" "hm_width")} ;;  
              {:signal "dend_width"}],
       :as ~(if left?
              ["y" "x" "depth" "children"]
              ["x" "y" "depth" "children"])}]}
    {:name ~(str name "-leaf")
     :source ~name
     :transform [{:type "filter" :expr "datum.children == 0"}]}]
  )


(defn spec
  [h-items v-items]
  `{:description "A clustered heatmap with side-dendrograms",
    :width 600 :height 600
    :$schema "https://vega.github.io/schema/vega/v5.json",
    :layout {:align "each"
             :columns 2}
    :data [{:name "hm",
            :url "sheatmap.json"}
           ~@(tree-data-spec "ltree" "dend-real-g.json" true)
           ~@(tree-data-spec "utree" "dend-real-s.json" false)
           ]
    :scales
    ;; Note: min is because sorting apparently requires an aggregation? And there's no pickone
    [{:name "sx" :type "band" :domain  {:data "utree-leaf" :field "id" :sort {:field "x" :op "min"}} :range {:step 20} } 
     {:name "sy" :type "band" :domain  {:data "ltree-leaf" :field "id" :sort {:field "y" :op "min"}} :range {:step 20}} 
     {:name "color"
      :type "linear"
      :range {:scheme "BlueOrange"}
      :domain {:data "hm", :field "value"},
      }]
    :signals
    [{:name "labels", :value true, :bind {:input "checkbox"}}
     {:name "hm_width" :value ~(* 20 8)} ;TODO 20 x counts
     {:name "hm_height" :value ~(* 20 20)}
     {:name "dend_width" :value 60}]
    :padding 5,
    :marks
    [
     {:type :group                       ;Empty quadrant
      :style :cell
      :encode {:enter {:width {:signal "dend_width"},
                       :height {:signal "dend_width"}
                       :strokeWidth {:value 0}
                       }
               }
      }

     ;; V tree
     ~(tree "utree" false)

     ;; H tree
     ~(tree "ltree" true)

     ;; actual hmap 
     {:type "group"
      :name "heatmap"
      :style "cell"
      :encode {
               :update {
                        :width {:signal "hm_width"}
                        :height {:signal "hm_height"}
                        }
               },

      :axes
      [{:orient :right :scale :sy }     ;TODO titles
       {:orient :bottom :scale :sx :labelAngle 90 :labelAlign "left"}]

      :legends
      [{:fill :color
        :type :gradient
        ; :title "Median feature value"
        :titleOrient "bottom"
        :gradientLength {:signal "hm_height / 2"}
        }]

      :marks
      [{:type "rect"
        :from {:data "hm"}
        :encode
        {:enter
         {:y {:field "gene" :scale "sy"}
          :x {:field "sample" :scale "sx"}
          :width {:value 19} :height {:value 19}
          :fill {:field "value" :scale "color"}
          }}}
       ]
      }
     ]}) 

(def samples
  ["517" "513" "509" "521" "516" "520" "508" "512"])

(def genes
  ["ZBTB16"
   "FAM107A"
   "SPARCL1"
   "CACNB2"
   "HIF3A"
   "TIMP4"
   "WNT2"
   "PRSS35"
   "VCAM1"
   "NEXN"
   "DUSP1"
   "MT2A"   
   "STEAP2"
   "DNM1"
   "ADAM12"
   "ACSS1"
   "PDPN"
   "MAOA"
   "FGD4"
   "DNAJB4"
   ])

(defn dendrogram
  []  
  [v/vega-view (spec samples genes) []])

(defn ui
  []
  [:div.p-5
   [dendrogram]])
