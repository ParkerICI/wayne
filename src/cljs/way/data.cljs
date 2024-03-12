(ns way.data
  (:require [re-frame.core :as rf]
            [way.api :as api]
            [reagent.dom]
            )
  )

;;; Causes a new data fetch.
(rf/reg-event-db
 :set-param
 (fn [db [_ data-id param value]]		
   (prn :set-param data-id param value)
   (rf/dispatch [:fetch data-id])
   (when (= data-id :universal-meta)    ;TODO temp nongeneral hack, need dependency mgt
     (rf/dispatch [:fetch :universal]))
   (if (vector? param)                  ;? smell
     (assoc-in db (concat [:params data-id] param) value)
     (assoc-in db [:params data-id param] value))))

(rf/reg-sub
 :param
 (fn [db [_ data-id param]]
   (get-in db [:params data-id param])))

(rf/reg-event-db
 :fetch
 (fn [db [_ data-id]]
   (api/ajax-get "/api/v2/data" {:params (assoc (get-in db [:params data-id])
                                                :data-id data-id
                                                :filter (get-in db [:params :universal-meta :values]) ;TODO temp nongeneral hack until I think of something better

                                                )
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

(defmulti loaded (fn [id data db] id))

(defmethod loaded :default
  [id data db]
  (prn "no loaded for" id))

(rf/reg-event-db
 ::loaded
 (fn [db [_ data-id data]]
   (loaded data-id data db)
   (-> db
       (assoc-in [:data data-id] data)
       (assoc :loading? false))))
