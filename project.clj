(defproject wayne "0.1.0-SNAPSHOT"
  :description "Prototyping BRUCE website"
  :min-lein-version "2.0.0"
  :plugins [[lein-shadow "0.4.1"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.candelbio/multitool "0.1.7"]
                 [org.clojure/data.json "2.5.0"]
                 [environ "1.2.0"]
                 [com.taoensso/timbre "6.3.1"]
                 [com.hyperphor/way "0.1.6"] 
                 ;;; Dev only (TODO use profiles)
                 [seesaw "1.5.0"]

                 ;; frontend
                 ;; See packge.json for the real ones
                 
                 ;; This should get inherited from way, but is necessary for some reason
                 [thheller/shadow-cljs "2.26.5"] ;TODO maybe only in dev profile

                 ;;; Wayne specific
                 [com.google.cloud/google-cloud-bigquery "2.27.0"]
                 ]
  :main ^:skip-aot wayne.core
  :target-path "target/%s"
  :source-paths ["src/cljc" "src/clj" "src/cljs"] 
  :clean-targets ^{:protect false} [".shadow-cljs" "resources/public/cljs-out" "target"]
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
                       :modules {:dev-main {:entries [wayne.frontend.core wayne.frontend.munson]}}
                       :devtools {:preloads [day8.re-frame-10x.preload.react-18]}
                       :dev {:compiler-options
                             {:closure-defines
                              {re-frame.trace.trace-enabled?        true
                               day8.re-frame-10x.show-panel         false ;does not work, afaict
                               day8.re-frame.tracing.trace-enabled? true}}}}}}

  )
