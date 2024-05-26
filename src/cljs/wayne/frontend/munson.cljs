(ns wayne.frontend.munson
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [wayne.frontend.universal :as universal]

   #_ [wayne.frontend.signup :as signup]

   [org.candelbio.multitool.core :as u]
   ))


;;; This is universal.cljs, but adapted to run in Munson website.


(defn app-ui
  []
  [:div
   [universal/ui]])

(defn ^:dev/after-load mount-root
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (rf/clear-subscription-cache!)
  (let [root (createRoot (gdom/getElement "app"))]
    (.render root (r/as-element [app-ui]))
    )
  )

(defn ^:export init
  [& user]
  (rf/dispatch-sync [::initialize-db])
  (mount-root)
  )
