(ns wayne.frontend.app
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [org.candelbio.multitool.core :as u]
            [wayne.frontend.web-utils :as wu]
            ))

(defn header
  []
  [:div.header
   [:h1 [:a.ent {:href "/"} "Wayne/PharmaKB"]]
   [:nav.navbar.navbar-expand-lg
    [:ul.navbar-nav.mr-auto
     #_ (type-dropdown)              ;TODO move this whole thing into sss, duh
     #_ (ops-dropdown)
     #_ (refresh-button)              ;TODO dev only
     #_ (schema-link)
     (when @(rf/subscribe [:loading?])
       (wu/spinner 2))
     #_ (doc-item)
     ]]
   ])


