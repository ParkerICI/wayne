(ns wayne.core
  (:gen-class)
  (:require [hyperphor.way.server :as server]
            [hyperphor.way.config :as config]
            [wayne.handler :as handler]
            [org.candelbio.multitool.cljcore :as ju]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defn heroku-deploy-hack
  []
  (when-let [creds (env/env :google-credentials)]
    (spit "google-credentials.json" creds)
    ))

(defn -main
  [& args]
  (config/read-config "config.edn")
  (let [port (or (first args) (env/env :port) )]
    (log/info "Starting server on port" port)
    (heroku-deploy-hack)
    (server/start (Integer. port) (handler/app))
    ;; Smart enough to be a no-op on server
    (ju/open-url (format "http://localhost:%s" port))
    ))
