(ns wayne.frontend.signup
  (:require [re-frame.core :as rf]))

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

;;; Not used, testing the modal thing. But no way to detect when form is submitted? TODO
(defn signup-button
  []
  [:button.btn.btn-outline-primary
   {:on-click #(do (rf/dispatch [:modal {:show? true :contents signup}]))}
   "Signup"])


