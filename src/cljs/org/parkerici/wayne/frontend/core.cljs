(ns org.parkerici.wayne.frontend.core
  (:require
   [com.hyperphor.way.tabs :as tabs]
   [com.hyperphor.way.ui.init :as init]
   [org.parkerici.wayne.frontend.app :as app]   
   [com.hyperphor.way.flash :as flash]
   [com.hyperphor.way.modal :as modal]

   [org.parkerici.wayne.frontend.x.cohort :as cohort]
   [org.parkerici.wayne.frontend.x.sites :as sites]
   [org.parkerici.wayne.frontend.x.patients :as patients]
   [org.parkerici.wayne.frontend.x.fgrid :as fgrid]

   [org.parkerici.wayne.frontend.signup :as signup]
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
   [:p
    [:a {:href "pages/query-builder.html"} "Real site"]
    ]
   ])

(defn app-ui
  []
  [:div
   [modal/modal]
   [app/header]
   [flash/flash]
   [tabs/tabs
    :tab
    (array-map
     :home about
     :signup signup/signup
     :cohort cohort/ui
     :sites sites/sites
     :patients patients/patients
     :metadata patients/metadata-full
     ;; :dendrogram dend/dev-ui
     :data_grid fgrid/ui
     )]
   ])

(defn ^:export init
  []
  (init/init app-ui nil))

