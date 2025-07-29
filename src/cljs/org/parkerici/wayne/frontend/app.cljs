(ns org.parkerici.wayne.frontend.app
  (:require [re-frame.core :as rf]
            [com.hyperphor.way.web-utils :as wu]
            ))

(defn header
  []
  [:div.header
   [:h1 "Sketches for BRUCE Data Portal"
    [:span.m-3 
     (when @(rf/subscribe [:loading?])
       (wu/spinner 1))]
    ]])

;;; TODO this patches a huge bug in Way, should find its way (hah) back there
(rf/reg-sub
 :datax
 (fn [db [_ data-id params]]
   (let [data (get-in db [:data data-id])
         status (get-in db [:data-status data-id])
         last-params (get-in db [:data-params data-id])
         invalid? (or (nil? data)
                      (= status :invalid)
                      (not (= params last-params))
                      )]
     (prn :get-data-sup data-id status invalid?)
     (when-not (or (= status :error) (= status :fetching))
       (when invalid?
         (rf/dispatch [:fetch data-id params])))
     data)))




   


