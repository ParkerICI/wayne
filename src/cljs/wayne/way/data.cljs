(ns wayne.way.data
  (:require [re-frame.core :as rf]
            [wayne.way.api :as api]
            [reagent.dom]
            )
  )

(rf/reg-event-db
 ::loaded
 (fn [db [_ data-id data]]
   #_ (do-vega (violin data "ROI"))        ;TODO generalize dim, can be "immunotherapy" (but needs label)
   (-> db
       (assoc-in [:data data-id] data)
       (assoc :loading? false))))

(rf/reg-event-db
 :fetch
 (fn [db [_ data-id]]
   (api/ajax-get "/api/v2/data" {:params {:data-id data-id}
                                 :handler #(rf/dispatch [::loaded data-id %])
                                 })
   (assoc db :loading? true)))

(rf/reg-event-db
 :fetch-once
 (fn [db [_ data-id]]
   (when-not (get-in db [:data data-id])
     (rf/dispatch [:fetch data-id]))))

(rf/reg-sub
 :data
 (fn [db [_ data-id]]
   (get-in db [:data data-id])))
