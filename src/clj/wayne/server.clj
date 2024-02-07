(ns wayne.server
  (:require [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log]
            [wayne.handler :as handler]))

(def server (atom nil))

(defn stop
  []
  (when @server
    (.stop @server)))

(defn start
  ([port] (start port handler/app))
  ([port handler]
   (log/infof "Starting wayne server at port %s" port)
   (stop)
   (reset! server (jetty/run-jetty handler {:port port :join? false}))))

  






