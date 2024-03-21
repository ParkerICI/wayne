(ns way.data
  (:require [re-frame.core :as rf]
            [way.api :as api]
            [reagent.dom]
            [org.candelbio.multitool.core :as u]
            )
  )

;;; → Multitool, maybe, kind of silly. Would make more sense if you could include lists or generators in a path.
(defn assoc*
  [m k v]
  (if (= k :*)
    (u/map-values (fn [_] v) m)
    (assoc m k v)))

(defn assoc-in*
  [m [k & ks] v]
  (if ks
    (assoc* m k (assoc-in* (get m k) ks v))
    (assoc* m k v)))

;;; This completely does not work
(defn invalidate
  [db data-id param]
  ;; Lazy, invalidate everything
  (assoc-in* db [:data-status :*] :invalid))

;;; Causes a new data fetch.
(rf/reg-event-db
 :set-param
 (fn [db [_ data-id param value]]		
   (prn :set-param data-id param value)
   (rf/dispatch [:fetch data-id])       ;timing issues?
   ;; Perhaps no longer needed?
   #_ (when (= data-id :universal-meta)    ;TODO temp nongeneral hack, need dependency mgt
     (rf/dispatch [:fetch :universal]))
   (-> (if (vector? param)                  ;? smell
         (assoc-in db (concat [:params data-id] param) value)
         (assoc-in db [:params data-id param] value))
       (invalidate data-id param))))    ;TODO completely not working and the wrong thing

(rf/reg-sub
 :param
 (fn [db [_ data-id param]]
   (if (vector? param)
     (get-in db (concat [:params data-id] param))
     (get-in db [:params data-id param]))))

;;; Slightly dekludged, needs to be a multimethod or something, and be in Wayne where it belongs TODO
;;; Also totally wrong, doesn't deal with dependency issue! Probably should use subscribe
(defn extra-params
  [db data-id]
  (case data-id
    :universal {:filter (get-in db [:params :universal-meta :filters] {})}
    :heatmap {:filter (get-in db [:params :universal-meta :filters] {})
              :dim (get-in db [:params :universal :dim])}))



(rf/reg-event-db
 :fetch
 (fn [db [_ data-id]]
   (api/ajax-get
    "/api/v2/data"
    {:params (merge (get-in db [:params data-id])
                    (extra-params db data-id)
                    {:data-id data-id}
                    )
     :handler #(rf/dispatch [::loaded data-id %])
     :error-handler #(rf/dispatch [:data-error data-id %1]) ;Override standard error handler
     })
   (assoc db :loading? true)))

(rf/reg-event-db
 :data-error
 (fn [db [_ data-id message]]
   ;; TODO temp, for debugging and old times sake
   (rf/dispatch [:flash (if message
                        {:class "alert-danger" :message message}
                        {:show? false})])
   (-> db
       (assoc-in [:data-status data-id] :error)
       (assoc-in [:data-status-error data-id] message) ;TODO nothing looks at this yet
       (assoc :loading? false))))

(rf/reg-event-db
 :fetch-once
 (fn [db [_ data-id]]
   (when-not (get-in db [:data data-id])
     (rf/dispatch [:fetch data-id]))))

(rf/reg-sub
 :data
 (fn [db [_ data-id]]
   (let [data (or (get-in db [:data data-id]) [])]
     (case (get-in db [:data-status data-id])
       :valid data
       :fetching data
       :error []
       (:invalid nil) (do (rf/dispatch [:fetch data-id])
                          data)))))

(defmulti loaded (fn [id data db] id))

(defmethod loaded :default
  [id data db]
  (prn "no loaded for" id)) 

(rf/reg-event-db
 ::loaded
 (fn [db [_ data-id data]]
   (loaded data-id data db)             ;mm
   (-> db
       (assoc-in [:data data-id] data)
       (assoc-in [:data-status data-id] :valid) ;not necessarily, UI could have changed while we were loading!
       (assoc :loading? false))))
