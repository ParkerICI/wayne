(ns wayne.frontend.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [way.tabs :as tabs]

   [wayne.frontend.app :as app]   
   [way.flash :as flash]
   [way.modal :as modal]

   ;; TODO belongs in app probably oh well
   ;; [wayne.frontend.violins :as violins]
   ;; [wayne.frontend.patients :as patients]
;;   [wayne.frontend.sites :as sites]
   [wayne.frontend.cohort :as cohort]
   [wayne.frontend.samples :as samples]
   ;; [wayne.frontend.dotplot :as dotplot]
   ;; [wayne.frontend.barchart :as barchart]
   [wayne.frontend.universal :as universal]
   ;; [wayne.frontend.dendrogram :as dend]
   ;; [wayne.frontend.fgrid :as fgrid]
   [wayne.frontend.signup :as signup]

   [org.candelbio.multitool.core :as u]
   [org.candelbio.multitool.browser :as browser]
   )) 

(def debug?
  ^boolean goog.DEBUG)

(defn about
  []
  [:div.p-3
   [:p
    "Experiments towards a BRUCE Data Portal."]
   [:p
    [:a {:href "https://github.com/mtravers/wayne"} "Source"]]
   [:p
    [:a {:href "https://docs.google.com/document/d/1VuoqIaiXaNTA9ROrQFZJYRUNOEk8A5zADDDVuE8wlyc/edit"} "Requirements document"]
    ]
   [:p
    [:a {:href "https://docs.google.com/document/d/1W4D8Pi9S_xJDzcQkDXHmcbQRFtkT-xgQEFnWnADJWaw/edit?usp=sharing"} "Design document"]
    ]
   #_
   [:ul
    [:li ]
    ]])

(defn app-ui
  []
  [:div
   ;; TODO not used (but maybe for "login")
   [modal/modal]
   [app/header]
   [flash/flash]
   [tabs/tabs
    :tab
    (array-map
     :home about
     :signup signup/signup
     :cohort cohort/ui
     ;; :sites sites/sites
     ;; :patients patients/patients
     :sample_metadata samples/samples
;     :violin violins/violins
     ;; :dotplot dotplot/plot
     ;; :barchart barchart/plot
     :query universal/ui
     ;; :dendrogram dend/dev-ui
     ;; :data_grid fgrid/ui
     )]
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
    )
  )

(defn ^:export init
  [& user]
  (let [params (browser/url-params)]
    (rf/dispatch-sync [::initialize-db])
    #_ (nav/start!)
    )
  (mount-root)
  )

