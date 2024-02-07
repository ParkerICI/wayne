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

(def data-url "https://gist.githubusercontent.com/mtravers/ab8e57a71cd5e9f5766e1e66679451a3/raw/92da78c66d537ba66b020a41a158c61df458d469/boppop.csv")

(defn minimal
  []
  [:div
   [:h2 "Hello sailor"]
   [:form
    [:label {:for "url"} "URL"]
    [:input {:name "url"
             :id "url"}]
    ]])
   


