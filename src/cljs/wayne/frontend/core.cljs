(ns wayne.frontend.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [wayne.way.tabs :as tabs]

   [wayne.frontend.app :as app]   
   [wayne.way.flash :as flash]
   [wayne.way.modal :as modal]

   ;; TODO belongs in app probably oh well
   [wayne.frontend.violins :as violins]
   [wayne.frontend.patients :as patients]
   [wayne.frontend.sites :as sites]
   [wayne.frontend.samples :as samples]
   [wayne.frontend.scatter :as scatter]

   [org.candelbio.multitool.core :as u]
   [clojure.string :as str]
   [org.candelbio.multitool.browser :as browser]
   )) 

(def debug?
  ^boolean goog.DEBUG)

(defn about
  []
  [:div.p-3 "Experiments towards a BRUCE Data Portal."
   [:ul
    [:li [:a {:href "https://docs.google.com/document/d/1W4D8Pi9S_xJDzcQkDXHmcbQRFtkT-xgQEFnWnADJWaw/edit?usp=sharing"} "Design document"]]
    ]])

(defn app-ui
  []
  [:div
   [modal/modal]
   [app/header]
   [flash/flash]
   [tabs/tabs
    :tab
    {:home about
     :sites sites/sites
     :patients patients/patients
     :samples samples/samples
     :violin violins/violins
     :scatter scatter/plot
     }]
   #_ [footer]
   ])

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   {:app "wayne"
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

