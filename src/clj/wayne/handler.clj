(ns wayne.handler
  (:require [compojure.core :refer [defroutes context GET POST make-route routes]]
            [com.hyperphor.way.handler :as wh]
            [com.hyperphor.way.views.html :as html]
            [ring.util.response :as response]
            [wayne.qgen :as qgen]
            )
  )

(defroutes site-routes
  (GET "/" [] (response/redirect "pages/query-builder.html"))
  (GET "/x" [] (wh/spa))
  (GET "/sm-popout" [] (wh/spa
                        :main "wayne.frontend.sample_dist.sample_matrix_popout"
                        :title "BRUCE: Sample Distribution Matrix"))
  )

(defroutes api-routes
  ;; Could maybe be done under data, but seems kind of different
  (context "/api" []
    (GET "/querygen" [query] (wh/content-response (qgen/endpoint query)))))

(defn app
  []
  (wh/app site-routes api-routes))

