(ns wayne.frontend.app
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.web-utils :as wu]
            ))

(defn header
  []
  [:div.header
   [:h1 [:a.ent {:href "/"} "Wayne's World"]]
   [:nav.navbar.navbar-expand-lg
    [:li.nav-item
     [:a {:href "/about"} "About"]
     ]
    [:ul.navbar-nav.mr-auto
     #_
     (when @(rf/subscribe [:loading?])
       (wu/spinner 2))
     ]]
   ])

(defn minimal
  []
  [:div
   [:h2 "Hello sailor"]
   [:form
    [:label {:for "url"} "URL"]
    [:input {:name "url"
             :id "url"}]
    ]])
   


