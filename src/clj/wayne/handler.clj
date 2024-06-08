(ns wayne.handler
  (:require [compojure.core :refer [defroutes context GET POST make-route routes]]
            [compojure.route :as route]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [hyperphor.way.views.html :as html]
            [hyperphor.way.views.admin :as admin]
            [hyperphor.way.handler :as wh]
            [wayne.data :as data]
            [ring.logger :as logger]
            [ring.middleware.session.memory :as ring-memory]
            [ring.middleware.resource :as resource]
            [taoensso.timbre :as log]
            [ring.middleware.defaults :as middleware]
            [ring.util.response :as response]
            [clojure.string :as str]
            [environ.core :as env]
            )
  (:use [hiccup.core])
  )

;;; Standin sample view
(defn sample-view
  [id]
  (response/content-type
   (content-response
    (html/html-frame
     {} (str "Sample " id)
     [:h3 "Imagine a Vitessce view of " id " here."]
     ))
   "text/html"))

;;; TODO re-integrate
(defroutes site-routes
  (GET "/" [] (spa))                    ;index handled by spa
  ;; For dev only, currently is a security hole
  #_ (GET "/admin" req (admin/view req))
  (GET "/sample/:id" [id] (sample-view id) )
  (GET "*" [] (spa))                    ;default is handled by spa
  (route/not-found "Not found")         ;TODO this  will never be reached? But spa should do something reasonable with bad URLs
  )


(def app wh/app)

