;;; Based on Catamite and Traverse. Trying to keep this usable as a template

(defproject wayne "0.1.0-SNAPSHOT"
  :description "Prototyping BRUCE website"
  :min-lein-version "2.0.0"
  :plugins [[lein-shadow "0.4.1"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.candelbio/multitool "0.1.5"]
                 [org.clojure/data.json "2.5.0"]
                 [environ "1.2.0"]
                 [com.taoensso/timbre "6.3.1"]
                 [hyperphor/way "0.1.0-SNAPSHOT"]

                 ;; Backend
                 [clj-http "3.12.3" :exclusions [commons-io]]
                 [compojure "1.7.0"]
                 [ring "1.11.0"]
                 [ring/ring-core "1.11.0"]
                 [ring/ring-defaults "0.4.0"]
                 [ring/ring-jetty-adapter "1.11.0"]
                 [ring-basic-authentication "1.2.0"]
                 [ring-logger "1.1.1"]
                 [ring-middleware-format "0.7.5" :exclusions [javax.xml.bind/jaxb-api]]
                 ;; Data
                 [clj-http "3.12.3" :exclusions [commons-io]]
                 [alandipert/enduro "1.2.0"] ;persistence for expensive calculations
                 ;; frontend
                 ;; See packge.json for the real ones
                 #_ [org.clojure/clojurescript "1.11.132"] ;causes shadow-cljs error, who knows
                 [thheller/shadow-cljs "2.26.5"] ;TODO maybe only in dev profile
                 [reagent "1.2.0"]
                 [re-frame "1.4.2"]
                 [com.cemerick/url "0.1.1"]
                 [cljs-ajax "0.8.0"]
                 [day8.re-frame/tracing "0.6.2"]      ;TODO dev only
                 [day8.re-frame/re-frame-10x "1.9.3"] ;TODO dev only

                 ;;; Dev only (TODO use profiles)
                 [seesaw "1.5.0"]

                 ;;; Wayne specific

                 [com.google.cloud/google-cloud-bigquery "2.27.0"]
                 [com.google.cloud/google-cloud-datastore "2.14.7"]
                 [com.google.cloud/google-cloud-storage "2.22.3"]

                 ]
  :main ^:skip-aot wayne.core
  :target-path "target/%s"
  :source-paths ["src/cljc" "src/clj" "src/cljs"] 
  :clean-targets [".shadow-cljs"]
  :repl-options {:init-ns wayne.core}
  :profiles {:uberjar {:aot :all
                       :omit-source true
                       :prep-tasks [["shadow" "release" "app"] "javac" "compile"] ;NOTE if you omit the javac compile items, :aot stops working!
                       :resource-paths ["resources"]
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :shadow-cljs {:lein true
                :builds
                {:app {:target :browser
                       :compiler-options {:infer-externs true}
                       :output-dir "resources/public/cljs-out"
                       :asset-path "/cljs-out"         ;webserver path
                       :modules {:dev-main {:entries [wayne.frontend.core]}}
                       :devtools {:preloads [day8.re-frame-10x.preload.react-18]}
                       :dev {:compiler-options
                             {:closure-defines
                              {re-frame.trace.trace-enabled?        true
                               day8.re-frame-10x.show-panel         false ;does not work, afaict
                               day8.re-frame.tracing.trace-enabled? true}}}}}}

  )
