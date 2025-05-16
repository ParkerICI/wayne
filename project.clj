(defproject org.parkerici.wayne "0.1.0-SNAPSHOT"
  :description "Prototyping BRUCE website"
  :min-lein-version "2.0.0"
  :plugins [[lein-shadow "0.4.1"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.candelbio/multitool "0.1.10"]
                 [com.hyperphor/way "0.1.13"] ;TODO not yet publically deployed
                 [org.clojure/data.json "2.5.0"]
                 [environ "1.2.0"]
                 [com.taoensso/timbre "6.3.1"]
                 ;;; Dev only (TODO use profiles)
                 [seesaw "1.5.0"]

                 ;; frontend
                 ;; See packge.json for the real ones
                 
                 ;; This should get inherited from way, but is necessary for some reason
                 [thheller/shadow-cljs "2.28.18"] ;TODO maybe only in dev profile

                 [com.google.cloud/google-cloud-bigquery "2.27.0"]
                 ]
  :main ^:skip-aot org.parkerici.wayne.core
  :target-path "target/%s"
  :source-paths ["src/cljc" "src/clj" "src/cljs"] 
  :clean-targets ^{:protect false} [".shadow-cljs" "resources/public/cljs-out" "target" "resources/public/pages"]
  :repl-options {:init-ns org.parkerici.wayne.core}
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
                       :modules {:dev-main {:entries [org.parkerici.wayne.frontend.core
                                                      org.parkerici.wayne.frontend.munson
                                                      org.parkerici.wayne.frontend.samples
                                                      org.parkerici.wayne.frontend.vitessce
                                                      org.parkerici.wayne.frontend.access
                                                      org.parkerici.wayne.frontend.sample-dist ;for popout
                                                      ]}}
                       :devtools {:preloads [day8.re-frame-10x.preload.react-18]}
                       :dev {:compiler-options
                             {:closure-defines
                              {re-frame.trace.trace-enabled?        true
                               day8.re-frame-10x.show-panel         false ;does not work, afaict
                               day8.re-frame.tracing.trace-enabled? true}}}}}}

  )
