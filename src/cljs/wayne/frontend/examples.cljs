(ns wayne.frontend.examples
  (:require
   [re-frame.core :as rf]
   [org.candelbio.multitool.core :as u]
   ))

;;; TODO vis parameters 

(rf/reg-event-db
 :remember-example
 (fn [db _]
   (assoc db
          :example
          (select-keys db [:params             ;includes query and feature selector param
                           ]))))

(rf/reg-event-db
 :recall-example
 (fn [db _]
   (u/merge-recursive db (:example db))))
