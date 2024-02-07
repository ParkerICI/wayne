(ns wayne.frontend.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [wayne.way.tabs :as tabs]

   [wayne.frontend.app :as app]   
   [wayne.frontend.flash :as flash]
   [wayne.frontend.modal :as modal]
   [wayne.frontend.violins :as violins]
   [wayne.frontend.patients :as patients]
   [org.candelbio.multitool.core :as u]
   [clojure.string :as str]
   [org.candelbio.multitool.browser :as browser]
   )) 

(def debug?
  ^boolean goog.DEBUG)

(defn about
  []
  [:div "Explanatory drivel"])

(defn app-ui
  []
  [:div
   [modal/modal]
   [app/header]
   [flash/flash]
   #_
   (case @(rf/subscribe [:page])        ;NOTE: this is useless and confused, flush or replace with real nav
     )
   #_ [app/minimal]
   [tabs/tabs
    :tab
    {:home about
     :violin violins/violins
     :patients patients/patients}]
   #_ [footer]
   ])

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   {:app "wayne"
    :page :home
    }))

(defn ^:dev/after-load mount-root
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (rf/clear-subscription-cache!)
  (let [root (createRoot (gdom/getElement "app"))]
    (.render root (r/as-element [app-ui]))
    ))

(defn ^:export init
  [& user]
  (let [params (browser/url-params)]
    (rf/dispatch-sync [::initialize-db])
    #_ (nav/start!)
    )
  (mount-root)
  )

