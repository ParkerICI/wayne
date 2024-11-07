(ns wayne.frontend.heatmap
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [com.hyperphor.way.web-utils :as wu]
            com.hyperphor.way.params
            [reagent.dom]
            [org.candelbio.multitool.math :as um]
            [com.hyperphor.way.cheatmap :as dendro]
            ))

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

(defn conjs
  [coll thing]
  (if (nil? coll)
    #{thing}
    (conj coll thing)))

(defn row
  [label contents]
  [:div.row.my-2
   [:div.col-3 [:label.small.pt-2 [:b label]]]
   [:div.col-9 contents]])

;;; This is actually part of the he
(defn feature-list-ui
  []
  (let [feature @(rf/subscribe [:selected-feature])
        feature-list @(rf/subscribe [:param :heatmap2 :feature-list])]
    [:div
     [row "variable to add:"
      [:span
       (wu/humanize feature)
       (when (not (contains? feature-list feature))
         [:button.btn.btn-sm.btn-secondary.mx-2 ;TODO none of these boostrap stules are present any more
          {:href "#"
           :on-click #(rf/dispatch [:update-param :heatmap2 :feature-list conjs feature])}
          "add"])
       ]]
     ;; TODO lozenge UI
     [row
      [:span "feature list:"
       (when-not (empty? feature-list)
         [:button.btn.btn-sm.btn-secondary.mx-2 {:href "#" :on-click #(rf/dispatch [:set-param :heatmap2 :feature-list #{}])} "clear"])]
      (str/join ", " (map wu/humanize feature-list))]]))


;;; TODO The "n rows, zeros omitted, Download" row doesn't really apply
(defn heatmap2
  [dim data]
  [:div
   [:fieldset {:style {:margin-top "5px" :height "auto"}} [:legend "feature selection"]
    [feature-list-ui]]
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
                        ;; :cluster-rows? false
                        ;; TODO labels should be on left in this case
                        :aggregate-fn :mean
                        :patches [[{:orient :bottom :scale :sx}
                                   {:labelAngle 90
                                    :labelFontSize 12
                                    :titleFontSize 14
                                    :titleAnchor :start ;without this, titles will grow the grid section and make it come loose from trees...prob should be default
                                    }]
                                  [{:orient :right :scale :sy}
                                   {:labelFontSize 12
                                    :titleFontSize 14
                                    :titleAnchor :start
                                    }]
                                  [{:fill :color :type :gradient}
                                   {:titleFontSize 14
                                    :gradientLength {:signal "max(hm_height,100)"}
                                    }
                                   ]
                                  ]}
                       )
       ))

   ])
