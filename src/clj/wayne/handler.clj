(ns wayne.handler
  (:require [compojure.core :refer [defroutes context GET POST make-route routes]]
            [com.hyperphor.way.handler :as wh]
            [com.hyperphor.way.views.html :as html]
            [ring.util.response :as response]
            )
  )

(defroutes site-routes
  (GET "/" [] (response/redirect "pages/query-builder.html"))
  (GET "/x" [] (wh/spa))
  )

(defn app
  []
  (wh/app site-routes (routes)))

