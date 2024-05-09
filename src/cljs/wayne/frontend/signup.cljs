(ns wayne.frontend.signup
  (:require [re-frame.core :as rf]
            [way.web-utils :as wu]))

;;; Idea: copy the actual HTML of the form and serve it up, then all the iframe security shit goes away.

(defn signup
  []
  [:div
   [:iframe {:src "https://docs.google.com/forms/d/e/1FAIpQLSepOPYiOF-WDZSBAd4DrrOVhdl_9VQv-4QkXN9ksJYN-pyx6Q/viewform?embedded=true"
             :width "80%" ; "640"
             :height "920"
             :frameBorder "0"
             :marginHeight "0"
             :marginWidth "0"}
    "Loading…"]])

(defn expose
  []
  (rf/dispatch [:modal {:show? true :contents signup}]))

;;; Not used, testing the modal thing. But no way to detect when form is submitted? TODO
(defn button
  []
  [:button.btn.btn-outline-primary
   {:on-click expose}
   "Signup"])


;;; Should be a simple way to say, here's a state variable but I want it backed in localStorage (or elsewhere)
(rf/reg-sub
 :registered?
 (fn [db _]
   (or (wu/get-local-storage "wayne.signup")
       (:registered? db))))

(rf/reg-event-db
 :register
 (fn [db _]
   (wu/set-local-storage "wayne.signup" "true")
   (assoc db :registered true)))

(defn with-signup
  [button]
  (if @(rf/subscribe [:registered?])
    button
    [:button.btn.btn-outline-primary
     {:on-click #(do
                   (.preventDefault %)
                   (expose)
                   (rf/dispatch [:register])
                   )}
     "Register to Download"]))


