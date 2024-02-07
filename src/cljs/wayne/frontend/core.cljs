(ns wayne.frontend.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [wayne.frontend.app :as app]   
   [wayne.frontend.flash :as flash]
   [wayne.frontend.modal :as modal]
   [org.candelbio.multitool.core :as u]
   [clojure.string :as str]
   [org.candelbio.multitool.browser :as browser]
   )) 

(def debug?
  ^boolean goog.DEBUG)

(rf/reg-sub
 :page
 (fn [db _]
   (:page db)))

(defn app-ui
  []
  [:div
   [modal/modal]
   [app/header]
   [flash/flash]
   #_
   (case @(rf/subscribe [:page])        ;NOTE: this is useless and confused, flush or replace with real nav
     )
   [app/minimal]

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

