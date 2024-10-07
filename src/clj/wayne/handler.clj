(ns wayne.handler
  (:require [compojure.core :refer [defroutes context GET POST make-route routes]]
            [com.hyperphor.way.handler :as wh]
            [com.hyperphor.way.views.html :as html]
            [ring.util.response :as response]
            )
  )

;;; Standin sample view
#_
(defn sample-view
  [id]
  (response/content-type
   (wh/content-response
    (html/html-frame
     {} (str "Sample " id)
     [:h3 "Imagine a Vitessce view of " id " here."]
     ))
   "text/html"))

(defroutes site-routes
  (GET "/" [] (response/redirect "pages/query-builder.html"))
  #_ (GET "/sample/:id" [id] (sample-view id) )
  )

(defn app
  []
  (wh/app site-routes (routes)))

