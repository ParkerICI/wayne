(ns wayne.templating
  (:require [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [wayne.team :as team]
            ))

;;; <<<>>> ⫷ Templating ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>>    

;; Alternative: Selmer but easier to roll my own https://github.com/yogthos/Selmer

;;; → multitool (java) but I wish it could give resource paths instead of files
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

(defn expand-template
  [template params]
  (u/expand-template template
                     params
                     :param-regex u/double-braces
                     :key-fn keyword))

(defn expand-page
  [page-text component-map]
  (expand-template page-text component-map))

;; Called at Uberjar build time. Won't work at runtime on Heroku
(defn expand-pages
  []
  (let [component-map (resource-map "templates/components")
        page-map (resource-map "templates/pages")]
    (ju/ensure-directory "resources/public/pages")
    (doseq [[page-key page-content] page-map]
      (log/info "Expanding page" page-key)
      (spit (str "resources/public/pages/" (name page-key) ".html")
            (expand-page page-content component-map)))))

(defn init
  []
  ;; Expand pages if they aren't present
  (when-not (io/resource "public/pages/about-us.html")
    (team/generate)
    (expand-pages)))

;;; Important: Causes expansion to happen at uberjar build time, for deployment
(init)


;;; <<<>>> ⫷ Component Detection ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>> ⫷ ⫸ <<<>>>

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

