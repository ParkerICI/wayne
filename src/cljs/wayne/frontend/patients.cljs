(ns wayne.frontend.patients
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [wayne.frontend.aggrid :as ag]
            [org.candelbio.multitool.core :as u]
            [wayne.way.tabs :as tab]
            [wayne.frontend.web-utils :as wu]
            [wayne.frontend.api :as api]
            [reagent.dom]
            [clojure.string :as str]
            )
  )

(rf/reg-event-db
 ::loaded
 (fn [db [_ data]]
   #_ (do-vega (violin data "ROI"))        ;TODO generalize dim, can be "immunotherapy" (but needs label)
   (assoc db
          :patients data
          :loading? false
          )))

(rf/reg-event-db
 ::fetch
 (fn [db _]
   (api/ajax-get "/api/v2/patients" {:params (:params db)
                                     :handler #(rf/dispatch [::loaded %])
                                     })
   (assoc db :loading? true)))

(rf/reg-sub
 :patients
 (fn [db _]
   (:patients db)))

(defn patients
  []
  [:div
   [:h3 "Patients"]
   (let [patients @(rf/subscribe [:patients])]
     [ag/ag-table 
      :patients
      (keys (first patients))
      patients
      {}
      ])])

(defmethod tab/set-tab [:tab :patients]
  [db]
  (when-not (:patients db)
    (rf/dispatch [::fetch])))
