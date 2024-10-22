(ns wayne.frontend.visualization
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [wayne.frontend.signup :as signup]
            [com.hyperphor.way.web-utils :as wu]
            [com.hyperphor.way.vega :as v]
            com.hyperphor.way.params
            [com.hyperphor.way.download :as download]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.math :as um]
            [wayne.frontend.feature-select :as fui]
            [com.hyperphor.way.cheatmap :as dendro]
            ))

(defn interpret-scale
  [scale]
  (case scale
    "log2" {:type "log" :base 2}
    "log10" {:type "log" :base 10}
     {:type scale}))

;;; Note :zindex attribute doesn't work, but ordering the marks does
(defn violin
  [data dim feature]
  (let [dim (name dim)
        scale (interpret-scale @(rf/subscribe [:param :features :scale]))] ;TODO wee fui/ref below
    {:description "A violin plot example showing distributions for pengiun body mass.",
     :$schema "https://vega.github.io/schema/vega/v5.json",
     :width 700,
     :signals
     [{:name "box", :value false #_ :bind #_ {:input "checkbox"}}
      {:name "points", :value true, #_ :bind #_  {:input "checkbox"}}
      {:name "jitter" :value 50 :bind {:input :range, :min 0, :max 200}}
      {:name "blobWidth", :value 200, :bind {:input :range, :min 100, :max 1000}} ;controls fatness of violins  
      {:name "blobSpace" :value 750 :bind {:input :range, :min 100, :max 2000}}
      {:name "height", :update "blobSpace"}
      {:name "trim", :value true, #_ :bind #_ {:input "checkbox"}}
      ;; TODO this didn't work, so going out of Vega
      #_ {"name" "xscales", "value" "linear" "bind"  {"input" "select" "options" ["linear" "log10" "log2" "sqrt"]}}
      {:name "bandwidth", :value 0, #_ :bind #_ {:input "range", :min 0, :max 1.0E-4, :step 1.0E-6}}],
     :data
     [{:name "source", :values data}
      {:name "density",
       :source "source",
       :transform
       [{:type "kde",                   ; Kernel Density Estimation, see https://vega.github.io/vega/docs/transforms/kde/
         :field "feature_value",
         :groupby [dim],
         :bandwidth {:signal "bandwidth"},
         :resolve "shared"
         #_ :extent #_ {:signal "trim ? null : [0.0003, 0.0005]"}}]}
      {:name "stats",
       :source "source",
       :transform
       [{:type "aggregate",
         :groupby [dim],
         :fields ["feature_value" "feature_value" "feature_value" "feature_value" "feature_value"],
         :ops ["min" "q1" "median" "q3" "max"],
         :as  ["min" "q1" "median" "q3" "max"]}]}]

     :config {:axisBand {:bandPosition 1, :tickExtra true, :tickOffset 0}},
     :axes
     [{:orient "bottom",
       :scale "xscale",
       :zindex 1,
       :labelFontSize 18 :titleFontSize 16
       :labelAngle 45 :labelAlign "left"
       :title (wu/humanize feature)} ;TODO want metacluster in this
      {:orient "left",
       :scale "layout",
       :tickCount 5,
       :labelFontSize 18 :titleFontSize 16
       :title dim
       :zindex 1}],

     :scales
     [{:name "layout",
       :type "band",
       :range "height",
       :domain {:data "source", :field dim},
       :paddingOuter 0.5}
      (merge
       {:name "xscale",
        :range "width",
        :round true,
        :domain {:data "source", :field "feature_value"},
        :nice true}
       scale)
      {:name "hscale",
       :type "linear",
       :range [0 {:signal "blobWidth"}],
       :domain {:data "density", :field "density"}}
      {:name "color", :type "ordinal", :domain {:data "source", :field dim}, :range "category"}],
     :padding 5,
     :marks
     [{:type "group",
       :from {:facet {:data "density", :name "violin", :groupby dim}},
       :encode
       {:update
        {:yc {:scale "layout", :field dim, :band 0.5},
         :height {:signal "blobWidth"},
         :width {:signal "width"}
         }},
       :data
       [{:name "summary",
         :source "stats",
         :transform [{:type "filter", :expr (wu/js-format "datum.%s === parent.%s" dim dim)}]}],
       :marks
       [

        {:type "area",                  ;Violins
         :from {:data "violin"},
         :encode
         {:enter {:fill {:scale "color", :field {:parent dim}}
                  },
          :update
          {:x {:scale "xscale", :field "value"},
           :yc {:signal "blobWidth / 2"},
           :opacity {:signal "box ? 0 : 1"}
           :height {:scale "hscale", :field "density"}}}}
        {:type "symbol",                ;Points
         :from {:data "source"},
         :encode
         {:enter {:y {:value 0}
                  
                  },
          :update
          {:stroke {:value "black"},
           :fill {:value "black"},
           :size {:value 25},
           :yc {:signal "blobWidth / 2 + jitter*(random() - 0.5)"}, ;should scale with fatness
           :strokeWidth {:value 1},
           :opacity {:signal "points ? 0.3 : 0"},
           :shape {:value "circle"},
           :x {:scale "xscale", :field "feature_value"}}}}
        
        #_
        {:type "rect",                  ;Box
         :from {:data "summary"},
         :encode
         {:enter {:cornerRadius {:value 4} },
          :update
          {:x {:scale "xscale", :field "q1"},
           :x2 {:scale "xscale", :field "q3"},
           :height {:signal "blobWidth / 10"}
           :yc {:signal "blobWidth / 2"}
           :fill {:scale "color", :field {:parent dim}}
           }}}
        {:type "rect",                  ;Box outline
         :from {:data "summary"},
         :encode
         {:enter {:stroke {:value "gray"}
                  :cornerRadius {:value 4}}
          :update
          {:x {:scale "xscale", :field "q1"},
           :x2 {:scale "xscale", :field "q3"},
           :height {:signal "blobWidth / 10"}
           :yc {:signal "blobWidth / 2"}
           #_ :fill #_ {:scale "color", :field {:parent dim}}
           }}}
        {:type "rect",                  ;Midpoint
         :from {:data "summary"},
         :encode
         {:enter {:fill {:value "white"}, :width {:value 2}, :height {:value 20}},
          :update {:x {:scale "xscale", :field "median"}, :yc {:signal "blobWidth / 2"}}}}

        {:type "rect",                  ;Whisker
         :from {:data "summary"},
         :encode
         {:enter {:fill {:value "black"}, :width {:value 2}, :height {:value 2}},
          :update {:x {:scale "xscale", :field "min"},
                   :x2 {:scale "xscale", :field "max"}
                   :yc {:signal "blobWidth / 2"}}}}


        ]}],
     }))

#_
(defn boxplot
  [data dim]
  (let [scale (interpret-scale @(rf/subscribe [:param :features :scale]))]
    {
     :$schema "https://vega.github.io/schema/vega-lite/v5.json",
     :data {:values data}
     :mark {:type "boxplot" :tooltip true}, ; :extent "min-max"
     :encoding {:x {:field "feature_value", :type "quantitative"
                    :scale scale},
                :y {:field dim :type "nominal"}
                ;; TODO this doesn't work, I think to get jitter you need to use Vega, in which case it really should be combined with violin plot I think
                ;; :yc {:signal "80*(random() - 0.5)"}
                :color {:field dim :type "nominal", :legend nil}
                }
     }))

;;; TODO use elsewhere. Or a different approach, but this works
(defn humanize-features
  [data]
  (map (fn [{:keys [feature_variable] :as row}]
         ;; Was adding :feature_human but then that shows up on axis lable
         (assoc row :feature_variable (wu/humanize feature_variable)))
       data))

(defn z-transform
  [ds field]
  (let [values (map field ds)
        mean (um/mean values)
        std (um/standard-deviation values)
        xvalues (map #(/ (- % mean) std) values)]
    (map (fn [row xformed] (assoc row field xformed))
         ds xvalues)))

;;; TODO belongs in way
(defn z-transform-columns
  [ds field column-field]
  (mapcat #(z-transform % field)
          (vals (group-by column-field ds))))

;;; TODO The "n rows, zeros omitted, Download" row doesn't really apply
(defn heatmap2
  [dim]
  (let [data (humanize-features @(rf/subscribe [:data :heatmap2]))]
    [:div
     (if (empty? data)
       [:div.alert.alert-info
        "No data, you probably need to add some features to the feature list"]
       (let [data (z-transform-columns data :mean :feature_variable)]
         ;; TODO the title or something should indicate z-score applied
         (dendro/heatmap data
                         dim
                         :feature_variable
                         :mean
                         {:color-scheme "magma"
                          ;; TODO :overrides, angle x labels
                          ;; :cluster-rows? false
                          ;; TODO labels should be on left in this case
                          ;; TODO color scale is too small.
                          :aggregate-fn :mean
                          :patches [[{:orient :bottom :scale :sx}
                                     {:labelAngle 45}]]}
                         )
         ))
     [fui/feature-list-ui]
     ]))

(defn scale-chooser
  []
  [:div.hstack.flex
   "Scale: " (fui/select-widget-minimal :scale ["linear" "log10" "log2" "sqrt" "symlog"])
   ])

(defn munson-tabs
  "Define a set of tabs. id is a keyword, tabs is a map (array-map is best to preserve order) mapping keywords to ui fns "
  [id tabs]
  (let [active (or @(rf/subscribe [:active-tab id])
                   (ffirst tabs))]      ;Default to first tab 
    [:div {:id id}
     [:div.tabs
      (for [[name view] tabs]
        ^{:key name}
        [:button.tab {:class (when (= name active) "active")
                      :on-click #(rf/dispatch [:choose-tab id name])
                      } (wu/humanize name)])]
     (when active
       ((tabs active)))]))

;;; I suppose this should be a sub?
(defn trim-zeros?
  ([]
   (= "marker_intensity" @(rf/subscribe [:param :features :feature-type])))
  ;; this version can be called in more places
  ([db]
   (= "marker_intensity" (get-in db [:params :features :feature-type])))
   )

(defn visualization 
  [dim feature data]
  (when dim
    [:div
     ;; TODO pluralize
     (if (empty? data)
       "No data"
       [:div.m-1
        [:span.mx-2 (str (count data) " rows")]
        (when (trim-zeros?)
          [:span.badge.text-bg-info "Zeros omitted"])   ; could do this but it is  wrong, and hides the actual 0-data case (if (empty? data) "No data" (str (count data) " rows")
        [:span.m-2 (signup/with-signup (download/button data (str "bruce-export-" feature ".tsv")))]])
     ;; TODO of course you might want to see these together, so tabs are not good design
     [munson-tabs                       ;TODO need to split or be an arg or simething
      :uviz
      (array-map
       :violin (fn [] [:div
                       [v/vega-view (violin data dim feature) data]
                       [scale-chooser]
                       ])
       #_ :boxplot #_ (fn [] [:div.vstack
                        [v/vega-lite-view (boxplot data dim) data]
                        [scale-chooser]
                        ])
       ;; :heatmap (fn [] [heatmap dim])
       :heatmap (fn [] [heatmap2 dim])
       ;; :dendrogram (fn [] [dendrogram dim])
       )]])
  )
