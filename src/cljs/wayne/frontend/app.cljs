(ns wayne.frontend.app
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.web-utils :as wu]
            ))

(defn header
  []
  [:div.header
   [:h1 "BRUCE features"]
   [:nav.navbar.navbar-expand-lg
    [:ul.navbar-nav.mr-auto
     ;; Note: no real items here, its all in tabs
     [:li.navbar-nav
      (when @(rf/subscribe [:loading?])
        (wu/spinner 2))
      ]]]])

;;; WAY
(rf/reg-sub
 :loading?
 (fn [db _]
   (:loading? db)))

(defn minimal
  []
  [:div
   [:h2 "Hello sailor"]
   [:form
    [:label {:for "url"} "URL"]
    [:input {:name "url"
             :id "url"}]
    ]])
   


