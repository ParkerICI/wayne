(ns wayne.frontend.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [wayne.frontend.app :as app]   
   [wayne.frontend.flash :as flash]
   [wayne.frontend.modal :as modal]
   [wayne.frontend.violins :as violins]
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

;;; For wu
(defn tabs
  [id tabs]
  (let [active (or @(rf/subscribe [:active-tab id]) "About")]
    (prn :foo id tabs active)
    [:div
     [:ul.nav.nav-tabs
      (for [[name view] tabs]
        ^{:key name}
        [:li.nav-item
         (if name
           [:a.nav-link {:class (when (= name active) "active")
                         :on-click #(rf/dispatch [:choose-tab id name])}
            name]
           [:a.nav-link.disabled.vtitle view])])]
     ((tabs active))]))

(rf/reg-sub
 :active-tab
 (fn [db [_ id]]
   (get-in db [:active-tab id])))

(rf/reg-event-db
 :choose-tab
 (fn [db [_ id tab]]
   (assoc-in db [:active-tab id] tab)))

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
   [tabs
    :tab
    {nil "Bruce"
     "About" violins/violins
     "Violin" violins/violins
     "Patients" #_ patients/patients [:h2 "Patients"]}]
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

