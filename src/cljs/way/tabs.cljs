(ns way.tabs
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [org.candelbio.multitool.core :as u]
   [clojure.string :as str]
   ))

;;; manages any kind of tabbed ui, or top level pages

(rf/reg-sub
 :page
 (fn [db _]
   (:page db)))

;;; For wu
(defn tabs
  [id tabs]
  (let [active @(rf/subscribe [:active-tab id])]
    (prn :tabs id active)
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
     (when active
       ((tabs active)))]))

(rf/reg-sub
 :active-tab
 (fn [db [_ id]]
   (get-in db [:active-tab id])))

(defmulti set-tab (fn [id tab db] [id tab]))

(defmethod set-tab :default
  [id tab db]
  (prn "no set-tab for" [id tab]))      ;Not an error, this might be very normal

(rf/reg-event-db
 :choose-tab
 (fn [db [_ id tab]]
   (set-tab id tab db)
   (assoc-in db [:active-tab id] tab)))
