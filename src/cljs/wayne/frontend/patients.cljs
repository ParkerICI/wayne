(ns wayne.frontend.patients
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [wayne.frontend.aggrid :as ag]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.web-utils :as wu]
            [wayne.frontend.api :as api]
            [reagent.dom]
            [clojure.string :as str]
            )
  )

(rf/reg-event-db
 :loaded
 (fn [db [_ data]]
   (do-vega (violin data "ROI"))        ;TODO generalize dim, can be "immunotherapy" (but needs label)
   (assoc db :loading? false)))

(rf/reg-event-db
 :fetch
 (fn [db _]
   (api/ajax-get "/api/v2/data0" {:params (:params db)
                                  :handler #(rf/dispatch [:loaded %])
                                  })
   (assoc db :loading? true)))


(defn patients
  []
  [:div
   [:h3 (:name @(rf/subscribe [::patients]))]
   [ag/ag-table 
    :sheet
    @(rf/subscribe [::columns]) 
    @(rf/subscribe [::rows])
    {}
    :checkboxes? false                 ;TODO They are there when we ready for them
    ]])
