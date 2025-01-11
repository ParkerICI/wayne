(ns wayne.frontend.visualization
  (:require [re-frame.core :as rf]
            [wayne.frontend.signup :as signup]
            [com.hyperphor.way.web-utils :as wu]
            [com.hyperphor.way.vega :as v]
            com.hyperphor.way.params
            [com.hyperphor.way.download :as download]
            [reagent.dom]
            [wayne.frontend.feature-select :as fui]
            [wayne.frontend.heatmap :as hm]
            ))

;;; Violin plot and framework. 

;;; TODO use elsewhere. Or a different approach, but this works
(defn humanize-features
  [data]
  (map (fn [{:keys [feature_variable] :as row}]
         ;; Was adding :feature_human but then that shows up on axis lable
         (assoc row :feature_variable (wu/humanize feature_variable)))
       data))

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
        scale (interpret-scale @(rf/subscribe [:param :features :scale]))]
    {:description "A violin plot"
     :$schema "https://vega.github.io/schema/vega/v5.json"
     :padding 5
     :width 800
     ;; :autosize :fit-x
     :signals
     [{:name "box" :value true :bind {:input "checkbox"  :element "#pchecks"}}
      {:name "violin" :value true :bind  {:input "checkbox" :element "#pchecks"}}
      {:name "points" :value true :bind  {:input "checkbox" :element "#pchecks"}}
      {:name "jitter"  :bind {:element "#jitter"}}
      {:name "blobWidthx" :bind {:element "#blobWidth"}} ;controls fatness of violins
      {:name "blobWidth" :update "parseInt(blobWidthx)"}             ;necessary because ext binding come in as string, bleah
      {:name "blobSpace" :bind {:element "#blobSpace"}}
      {:name "height" :value 800}
      {:name "width" :value 800 :update "blobSpace*1"} ; #_  "blobSpace * length(scale('dscale').domain)"}
      {:name "trim" :value true #_ :bind #_ {:input "checkbox"}}
      ;; TODO this didn't work, so going out of Vega. Note, see https://vega.github.io/vega/docs/signals/#bind-external
      #_ {"name" "vscales", "value" "linear" "bind"  {"input" "select" "options" ["linear" "log10" "log2" "sqrt"]}}
      {:name "bandwidth" :value 0 #_ :bind #_ {:input "range" :min 0 :max 1.0E-4 :step 1.0E-6}}]
     :data
     [{:name "source" :values data}
      {:name "density"
       :source "source"
       :transform
       [{:type "kde"                   ; Kernel Density Estimation, see https://vega.github.io/vega/docs/transforms/kde/
         :field "feature_value"
         :groupby [dim]
         :bandwidth {:signal "bandwidth"}
         :resolve "shared"
         #_ :extent #_ {:signal "trim ? null : [0.0003, 0.0005]"}}]}
      {:name "stats"
       :source "source"
       :transform
       [{:type "aggregate"
         :groupby [dim]
         :fields ["feature_value" "feature_value" "feature_value" "feature_value" "feature_value"]
         :ops ["min" "q1" "median" "q3" "max"],
         :as  ["min" "q1" "median" "q3" "max"]}]}]

     :config {:axisBand {:bandPosition 1, :tickExtra true, :tickOffset 0}},
     :axes
     [{:orient "left",
       :scale "vscale",
       :zindex 1,
       :labelFontSize 18 :titleFontSize 20
       ;; :labelAngle 90 :labelAlign "left"
       :title (wu/humanize feature)}

      {:orient "bottom",
       :scale "dscale",
       :ticks false
       :labelFontSize 18 :titleFontSize 20
       :labelPadding 7
       :title (wu/humanize (name dim))
       :zindex 1
       :labelAngle 90 :labelAlign "left"
       :encode
       {:labels
        ;; Replace _ with space in violin labels. Note: ? is to hide a bogus undefined row that refuses to go away
        {:update {:text {:signal "datum.value ? replace(datum.value, /_/, ' ') : ''"}}}},
       }],

     :scales
     [
      ;; dim values
      {:name "dscale",
       :type "band",
       :range "width",
       :domain {:data "source", :field dim :sort true},
       }

      ;; field values
      (merge
       {:name "vscale",
        :range "height",
        :round true,
        :domain {:data "source", :field "feature_value"},
        :nice true}
       scale)

      ;; Controls width of blobs
      {:name "hscale",
       :type "linear",
       :range [0 {:signal "blobWidth"}],
       :domain {:data "density", :field "density"}}

      ;; Color
      {:name "color",
       :type "ordinal",
       :domain {:data "source", :field dim},
       :range "category"}],

     :marks
     [{:type "group",
       :from {:facet {:data "density", :name "violin", :groupby dim}},
       :encode
       {:update
        {:xc {:scale "dscale", :field dim :band 0.5},
         :width {:signal "blobWidth"},
         :height {:signal "width"}
         }},

       :data
       [{:name "summary",
         :source "stats",
         :transform [{:type "filter", :expr (wu/js-format "datum.%s === parent.%s" dim dim)}]}],
       :marks
       [

        ;; Violins
        {:type "rect",                  ;should be area but doesn't work?
         :from {:data "violin"},
         :encode
         {:enter {:fill {:scale "color", :field {:parent dim}}
                  :tooltip {:signal "datum"}   ;TODO maybe 
                  },
          :update
          {:xc {:signal "blobWidth / 2"}
           :width {:scale "hscale", :field "density"}

           :y {:scale "vscale", :field "value"}
           :height {:value 50} #_ {:scale "hscale", :field "density"}

           :opacity {:signal "violin ? 1 : 0"}

           ;; :tooltip {:value "boxx"}
           }}}
        
        ;;  Box outline
        {:type "rect",                  
         :from {:data "summary"},
         :encode
         {:enter {:stroke {:value "black"}
                  :tooltip {:signal "datum"}
                  :cornerRadius {:value 4}}
          :update
          {:y {:scale "vscale", :field "q1"},
           :y2 {:scale "vscale", :field "q3"},
           :width {:signal "blobWidth / 5"}
           :xc {:signal "blobWidth / 2"}
           :opacity {:signal "box ? 1 : 0"}
           ;; If violins present, use black, otherwise semantic color. 
           :fill {:signal (str "violin ? '' :  scale('color', datum." dim ")")} 
           }}}

        ;; Midpoint
        {:type "rect",                  
         :from {:data "summary"},
         :encode
         {:enter {:fill {:value "black"},
                  :height {:value 2},
                  :width {:value 20}},
          :update {:y {:scale "vscale", :field "median"}
                   :xc {:signal "blobWidth / 2"}
                   :opacity {:signal "box ? 1 : 0"}
                   }}}



        ;; Whisker
        {:type "rect",                 
         :from {:data "summary"},
         :encode
         {:enter {:fill {:value "black"}, :width {:value 2}, :height {:value 2}},
          :update {:y {:scale "vscale", :field "min"},
                   :y2 {:scale "vscale", :field "max"}
                   :xc {:signal "blobWidth / 2"}
                   :opacity {:signal "box ? 1 : 0"}
                   }}}


        ]}

      ;; Points
      {:type "group",
       :from {:facet {:data "source", :name "points", :groupby dim}},
       :encode
       {:update
        {:xc {:scale "dscale", :field dim :band 0.5},
         }},
       :data [
              {:name "pointx"
               :source "points",
               ;; Add jitter here so stable when slider changed
               :transform [{:type "formula", :as "jit" :expr "random() - 0.5"}],               
               }]
       :marks
       [
        ;; Points
        {:type "symbol",
         :from {:data "pointx"},

         :encode
         {:enter {;; :y #_ {:value 0} {:field dim}
                  ;; Not very interesting
                  :tooltip {:signal "datum"}  
                  },
          :update
          {:stroke {:value "black"},
           :fill {:value "black"},
           :size {:value 25},
           :y {:scale "vscale", :field "feature_value"}
           :xc {:signal "jitter*datum.jit"}
           :strokeWidth {:value 1},
           :opacity {:signal "points ? 0.3 : 0"},
           :shape {:value "circle"},
           }}}]}
      ],
     }))


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

#_
(defn slider
  [& {:keys [id min max default]}]
  [:span
   [:label (str id)]
   ;; No :value, breaks interaction
   [:input {:id id :type "range" :name id :min min :max max :defaultValue default}] ;Step?
   ])

(defn update-slider-value
  [id]
  (fn [e]
    (let [value (.-value (.-target e))
          elt (.getElementById js/document id)
          sibling (.-nextSibling elt)]
      (set! (.-value sibling) value))))

(defn slider
  [& {:keys [id min max default]}]
  [:span.slider
   [:label (str id)]
   ;; No :value, breaks interaction
   [:input {:id id :type "range" :name id :min min :max max :defaultValue default
            :on-input (update-slider-value id)
            }] ;Step?
   [:output default]])

#_
(defn checkbox
  [& {:keys [id default]}]
  [:span
   [:label (str id)]
   ;; No :value, breaks interaction
   [:input {:id id :type "checkbox" :name id :defaultChecked default}]
   ])

(defn control-panel
  []
  [:table.table
   [:tr
    [:td 
     [:fieldset [:legend "Type"] [:div#pchecks]]]
    [:td 
     [:fieldset [:legend "Adjust plot"]
      [:table.table
       [:tr
        [:td [:span.slider "scale " (fui/select-widget-minimal :scale ["linear" "log10" "log2" "sqrt" "symlog"])]]
        [:td [slider :id "blobWidth" :min 50 :max 500 :default 100]]]
       [:tr
        [:td [slider :id "jitter" :min 0 :max 200 :default 25]]
        [:td [slider :id "blobSpace" :min 100 :max 2000 :default 700]] ;should be 150 and height = blobwidthh * domain, but not working
        ]]]]]])

(def saved-vega-params ["blobWidth" "blobSpace" "jitter" "scale" "box" "violin" "points"])

(defn visualization 
  [dim feature data]
  (when dim
    [:div
     (if (empty? data)
       "No data"
       [:div.m-1
        [:span.mx-2 (str (count data) " rows")]     ;; TODO pluralize
        (when (trim-zeros?)
          [:span.badge.text-bg-info "Zeros omitted"])   ; could do this but it is  wrong, and hides the actual 0-data case (if (empty? data) "No data" (str (count data) " rows")
        [:span.m-2 (signup/with-signup (download/button data (str "bruce-export-" feature ".tsv")))]])
     [munson-tabs
      :uviz
      (array-map
       :plot (fn [] [:div
                       [control-panel]
                     [v/vega-view
                      (violin data dim feature)
                      data
                      ;; This saves the vega signals in params db – but they aren't used yet. 
                      :listeners (zipmap saved-vega-params
                                         (repeat (fn [param v] (rf/dispatch [:set-param :violin param v]))))
                      ]
                       ])
       :heatmap (fn [] [hm/heatmap2 dim (humanize-features @(rf/subscribe [:data :heatmap2]))])
       )]])
  )
