(ns wayne.core
  (:gen-class)
  (:require [wayne.server :as server]
            [org.candelbio.multitool.cljcore :as ju]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defn heroku-deploy-hack
  []
  (when-let [creds (env/env :google-credentials)]
    (spit "google-credentials.json" creds)
    (prn :did-it-work (slurp  "google-credentials.json"))
    ))

(defn -main
  [& args]
  (let [port (env/env :port)]
    (log/info "Starting server on port" port)
    (heroku-deploy-hack)
    (server/start (Integer. port))
    ;; Smart enough to be a no-op on server
    (ju/open-url (format "http://localhost:%s" port))
    ))
