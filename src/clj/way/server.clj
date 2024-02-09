(ns way.server
  (:require [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log]))

(def server (atom nil))

(defn stop
  []
  (when @server
    (.stop @server)))

(defn start
  [port handler]
  (log/infof "Starting server at port %s" port) ;TODO name of app
  (stop)
  (reset! server (jetty/run-jetty handler {:port port :join? false})))

  






