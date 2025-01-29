(ns wayne.frontend.nlp
  (:require
   [re-frame.core :as rf]
   [org.candelbio.multitool.core :as u]
   [com.hyperphor.way.web-utils :as wu]
   [com.hyperphor.way.api :as api]
   
   ))



(def sample
  {:text  "Find the proportion of tumor cells that have high intensity B7H3 in GBM, Astrocytoma and Oligodendroglioma samples.",
   :params
   {:universal
 {:dim :Tumor_Diagnosis,
  :feature "B7H3_func_over_all_tumor_count_prop",
  :filters {:Tumor_Diagnosis {:GBM true, :Astrocytoma true, :Oligodendroglioma true}}},
 :features
 {:feature-bio-feature-type "Relative_to_all_tumor_cells",
  :feature-supertype "nonspatial",
  :scale "linear",
  :feature-broad_feature_type "Cells",
  :feature-feature_type "Cell_Abundance",
  :feature-feature_variable "B7H3_func_over_all_tumor_count_prop",
  :subfeature-0 "Macrophage_CD206",
  :subfeature-2 "Tcell_CD8"},
 :violin {:blobWidth 100, :blobSpace 530}}})

(defn nlp-ui
  []
   [:div
    "or try natural language!"
    [:br]
    [:textarea#nlquery {:style {:width 740}}] ;TODO manage through db so can be set from example, eg
    [:button {:on-click #(rf/dispatch [:nl-query (.-value (.getElementById js/document "nlquery"))])} "Go"]
    ])

;;; Start a nl query
(rf/reg-event-db
 :nl-query
 (fn [db [_ query-text]]
 ;; turn on spinner
   (api/ajax-get "/api/querygen" {:params {:query query-text}
                                  :handler (fn [response] (rf/dispatch [:nl-query-response response]))
                                  })
   #_ (rf/dispatch [:recall-example sample])
   db))

(rf/reg-event-db
 :nl-query-response
 (fn [db [_ response]]
 ;; turn off spinner
   (rf/dispatch [:recall-example response])
   db))
