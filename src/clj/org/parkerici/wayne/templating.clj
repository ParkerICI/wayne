(ns org.parkerici.wayne.templating
  (:require [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [org.parkerici.wayne.team :as team]
            ))

;;; <<<>>> ⫷ Templating ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>>    

;; Alternative: Selmer but easier to roll my own https://github.com/yogthos/Selmer

(defn get-resources
  [path]
  (->> path
       io/resource
       io/file
       (file-seq)
       (map #(.getPath %))
       ))

(defn resource-map
  [path]
  (let [comps
        (filter (fn [f] (str/ends-with? f ".html"))
                (get-resources path))]
    (zipmap (map #(keyword (second (re-find #"([^/]+)\.html$" %))) comps)
            (map slurp comps))))

(defn expand-component
  [component-template component-out params]
  (-> component-template
      (u/fsbl str "templates/components/")
      io/resource
      slurp
      (u/expand-template params :allow-missing? true)
      (u/fsbl spit (str "resources/templates/components/" component-out))))

(defn expand-components
  []
  (expand-component "nav.html" "nav-qb.html" {:active-qb "active-qb"})
  (expand-component "nav.html" "nav-samples.html" {:active-samples "active-samples"})
  (expand-component "nav.html" "nav-viz.html" {:active-viz "active-viz"})
  (expand-component "nav.html" "nav-about.html" {:active-about "active-about"})
  (expand-component "nav.html" "nav-rda.html" {:active-rda "active-rda"})
  )

;; Called at Uberjar build time. Won't work at runtime on Heroku
(defn expand-pages
  []
  (let [component-map (resource-map "templates/components")
        page-map (resource-map "templates/pages")]
    (ju/ensure-directory "resources/public/pages")
    (doseq [[page-key page-content] page-map]
      (log/info "Expanding page" page-key)
      (spit (str "resources/public/pages/" (name page-key) ".html")
            (u/expand-template page-content component-map)))))


(defn reinit
  []
  (team/generate)
  (expand-components)
  (expand-pages))

(defn init
  []
  ;; Expand pages if they aren't present
  (when-not (io/resource "public/pages/about-us.html")
    (reinit)))


;;; Important: Causes expansion to happen at uberjar build time, for deployment

(init)








;;; <<<>>> ⫷ Component Detection ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>>

(comment
  (def files ["about-us.html"
              "query-builder.html"
              "raw-data-access.html"
              "sample-info.html"
              "visualization.html"])

  (defn resource-lines
    [r]
    (->> r
         (str "public/pages/")
         io/resource
         io/reader
         line-seq
         (map str/trim)
         (filter #(> (count %) 30))))

  (defn read-files
    []
    (let [lines (mapcat resource-lines files)
          dupes (filter #(> (second %) 2) (frequencies lines))]
      dupes))
  )

