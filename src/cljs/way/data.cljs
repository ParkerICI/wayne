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
   (-> (if (vector? param)                  ;? smell
         (assoc-in db (concat [:params data-id] param) value)
         (assoc-in db [:params data-id param] value))
       ;; TODO not needed? All this needs to be rethought anyway
       #_ (invalidate data-id param))))

                                        ;TODO completely not working and the wrong thing

(rf/reg-event-db
 :set-param-if
 (fn [db [_ data-id param value]]		
   (prn :set-param-if data-id param value)
   (if (if (vector? param)
         (get-in db (concat [:params data-id] param))
         (get-in db [:params data-id param]))
     db
     (if (vector? param)                  ;? smell
       (assoc-in db (concat [:params data-id] param) value)
       (assoc-in db [:params data-id param] value))
     )))

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
    :heatmap {:filter (get-in db [:params :universal :filters] {})
              :dim (get-in db [:params :universal :dim])}
    {}))

;;; TODO get rid of this and just use a parameter map
(defn label-params
  [data-id]
  (if (vector? data-id)
    (if (map? (second data-id))
      (second data-id)
      (zipmap [:dim :feature :filters]
              (rest data-id)))
    {}))

(rf/reg-event-db
 :fetch
 (fn [db [_ data-id]]
   (let [event-params (label-params data-id)
         data-key (if (vector? data-id) (first data-id) data-id)]
     (api/ajax-get
      "/api/v2/data"
      {:params (merge (get-in db [:params data-id])
                      event-params
                      (extra-params db data-id)
                      {:data-id data-key} ;TODO fix terminology to be consistent
                      )
       :handler #(rf/dispatch [::loaded data-id %])
       :error-handler #(rf/dispatch [:data-error data-id %1]) ;Override standard error handler
       })
     (-> db
         (assoc :loading? true)
         (assoc-in [:data-status data-id] :fetching)
         ))))

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
       :fetching nil                   ; TODO unclear if it only means initial fetch or later ones
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
