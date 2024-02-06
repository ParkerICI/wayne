;;; Based on Catamite

(defproject wayne "0.1.0-SNAPSHOT"
  :description "Prototyping BRUCE website"
  :jvm-opts ["-Xmx12G"]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.candelbio/multitool "0.1.0"]
                 ;; This breaks gcs interface for some dependency shit, need to resolve
                 #_ [voracious "0.1.3"]
                 [me.raynes/fs "1.4.6"]
                 [com.google.cloud/google-cloud-bigquery "2.27.0"]
                 [com.google.cloud/google-cloud-datastore "2.14.7"]
                 [com.google.cloud/google-cloud-storage "2.22.3"]
                 ;; Might need exclusions, see rawsugar
                 [com.google.cloud/google-cloud-storage "1.75.0"]
                 #_ [com.google.auth/google-auth-library-oauth2-http "0.18.0"]
                 [metasoarous/oz "1.6.0-alpha36"]
                 [environ "1.1.0"]
                 ]
  :main wayne.core
  :repl-options {:init-ns wayne.core})

