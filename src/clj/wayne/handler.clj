(ns wayne.handler
  (:require [compojure.core :refer [defroutes context GET POST make-route routes]]
            [hyperphor.way.handler :as wh]
            [hyperphor.way.views.html :as html]
            [ring.util.response :as response]
            )
  )

;;; Standin sample view
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
  (GET "/sample/:id" [id] (sample-view id) )
  )

(def app (wh/app site-routes (routes)))

