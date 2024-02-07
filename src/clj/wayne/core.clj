(ns wayne.core
  (:gen-class)
  (:require [wayne.server :as server]
            [org.candelbio.multitool.cljcore :as ju]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defn -main
  [port & args]
    (log/info "Starting server on port" port)
    (server/start (Integer. port))
    ;; Smart enough to be a no-op on server
    (ju/open-url (format "http://localhost:%s" port))
  )
