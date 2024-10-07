(ns wayne.templating
  (:require #_[me.raynes.fs :as fs]
            [org.candelbio.multitool.core :as u]
            [org.candelbio.multitool.cljcore :as ju]
            [clojure.string :as str]
            [clojure.java.io :as io]))

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

(u/def-lazy component-map
  (resource-map "templates/components"))

(u/def-lazy page-map
  (resource-map "templates/pages"))

(defn expand-page
  [page-text]
  (u/expand-template page-text
                     @component-map
                     :param-regex u/double-braces
                     :key-fn keyword))

(defn write-resource
  [path content]
  (spit (str "resources/" path) content))

(defn expand-pages
  []
  (doseq [[page-key page-content] @page-map]
    (write-resource (str "public/pages/" (name page-key) ".html")
                    (expand-page page-content))))


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
