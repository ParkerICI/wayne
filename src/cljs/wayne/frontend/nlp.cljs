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
    "or try asking in natural language:"
    [:br]
    [:div {:style {:display :flex}}
     [:textarea#nlquery {:style {:width 540}}] ;TODO manage through db so can be set from example, eg
     [:button.btn {:on-click #(rf/dispatch [:nl-query (.-value (.getElementById js/document "nlquery"))])} "Generate"]
     (when @(rf/subscribe [:qgen?])
       [wu/spinner 2])]])

;;; Start a nl query
(rf/reg-event-db
 :nl-query
 (fn [db [_ query-text]]
   (api/ajax-get "/api/querygen" {:params {:query query-text}
                                  :handler (fn [response] (rf/dispatch [:nl-query-response response]))
                                  })
   (assoc db :qspinner true)))

(rf/reg-event-db
 :nl-query-response
 (fn [db [_ response]]
   (rf/dispatch [:recall-example response])
   (assoc db :qspinner false)))

(rf/reg-sub
 :qgen?
 (fn [db _]
   (get db :qspinner)))
