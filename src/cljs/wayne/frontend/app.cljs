(ns wayne.frontend.app
  (:require [re-frame.core :as rf]
            [way.web-utils :as wu]
            ))

(defn header
  []
  [:div.header
   [:h1 "Sketches for BRUCE Data Portal"
    [:span.m-3 
     (when @(rf/subscribe [:loading?])
       (wu/spinner 1))]
    ]])

;;; WAY
(rf/reg-sub
 :loading?
 (fn [db _]
   (:loading? db)))


   


